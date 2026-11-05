package com.waterleakage.app.data.repository

import android.content.Context
import com.waterleakage.app.data.model.ApiResult
import com.waterleakage.app.data.model.DashboardSummary
import com.waterleakage.app.data.model.HealthResponse
import com.waterleakage.app.data.model.ReadingRequest
import com.waterleakage.app.data.model.ReadingResponse
import com.waterleakage.app.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class WaterLeakageRepository(private val context: Context) {

    private val api get() = ApiClient.getApi(context)

    suspend fun checkHealth(): ApiResult<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.checkHealth()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(formatError(e), e)
        }
    }

    suspend fun submitReading(request: ReadingRequest): ApiResult<ReadingResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.createReading(request)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(formatError(e), e)
        }
    }

    suspend fun getReadings(filter: String? = null, search: String? = null): ApiResult<List<ReadingResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val list = api.getReadings(filter = filter, search = search)
                ApiResult.Success(list)
            } catch (e: Exception) {
                ApiResult.Error(formatError(e), e)
            }
        }

    suspend fun getReadingById(id: Int): ApiResult<ReadingResponse> = withContext(Dispatchers.IO) {
        try {
            val reading = api.getReadingById(id)
            ApiResult.Success(reading)
        } catch (e: Exception) {
            ApiResult.Error(formatError(e), e)
        }
    }

    suspend fun deleteReading(id: Int): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteReading(id)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(formatError(e), e)
        }
    }

    suspend fun getDashboardSummary(): ApiResult<DashboardSummary> = withContext(Dispatchers.IO) {
        try {
            val summary = api.getDashboardSummary()
            ApiResult.Success(summary)
        } catch (e: Exception) {
            ApiResult.Error(formatError(e), e)
        }
    }

    private fun formatError(throwable: Throwable): String {
        return when (throwable) {
            is IOException -> "Cannot connect to backend (${ApiClient.getBaseUrl(context)}). Please verify FastAPI server is running."
            else -> throwable.localizedMessage ?: "An unexpected error occurred."
        }
    }
}

