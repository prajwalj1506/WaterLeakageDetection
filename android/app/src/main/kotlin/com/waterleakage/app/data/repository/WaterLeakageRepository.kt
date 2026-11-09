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

    /**
     * Checks backend health with automatic multi-endpoint fallback:
     * If the current URL fails, it automatically probes candidate URLs (USB 127.0.0.1,
     * Wi-Fi LAN, Emulator) and silently switches to the working one!
     */
    suspend fun checkHealth(): ApiResult<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.checkHealth()
            ApiResult.Success(response)
        } catch (firstError: Exception) {
            // Auto-fallback: try candidate URLs
            val detected = ApiClient.autoDetectWorkingServer(context)
            if (detected != null) {
                try {
                    val response = api.checkHealth()
                    return@withContext ApiResult.Success(response)
                } catch (retryError: Exception) {
                    return@withContext ApiResult.Error(formatError(retryError), retryError)
                }
            }
            ApiResult.Error(formatError(firstError), firstError)
        }
    }

    suspend fun autoDetectServer(): ApiResult<String> = withContext(Dispatchers.IO) {
        val detected = ApiClient.autoDetectWorkingServer(context)
        if (detected != null) {
            ApiResult.Success(detected)
        } else {
            ApiResult.Error("Could not detect an active backend on USB (127.0.0.1), Wi-Fi, or Emulator.")
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
        val currentUrl = ApiClient.getBaseUrl(context)
        return when (throwable) {
            is IOException -> "Cannot reach backend ($currentUrl). Tap 'Auto-Connect' or check USB/Wi-Fi."
            else -> throwable.localizedMessage ?: "An unexpected error occurred."
        }
    }
}
