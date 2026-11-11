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
import java.net.ConnectException
import java.net.SocketTimeoutException

class WaterLeakageRepository(private val context: Context) {

    private val api get() = ApiClient.getApi(context)

    /**
     * Checks backend health with automatic multi-stage discovery fallback:
     * If the saved URL fails, it automatically probes USB, PC Hotspot, Emulator,
     * UDP broadcast discovery, and local /24 Wi-Fi subnets.
     */
    suspend fun checkHealth(): ApiResult<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.checkHealth()
            ApiResult.Success(response)
        } catch (firstError: Exception) {
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

    /**
     * Strictly tests a user-supplied URL without silently falling back to USB.
     * Normalizes bare IPs (e.g. "10.17.4.111" -> "http://10.17.4.111:8000/") and saves only on success.
     */
    suspend fun testAndSaveUrl(rawUrl: String): ApiResult<String> = withContext(Dispatchers.IO) {
        val normalized = ApiClient.normalizeUrl(rawUrl)
            ?: return@withContext ApiResult.Error(
                "Invalid server address. Enter an IP (e.g. 10.17.4.111) or full URL (e.g. http://192.168.1.5:8000/)."
            )

        val reachable = ApiClient.testHealthStrict(normalized, timeoutMs = 4500L)
        if (reachable) {
            ApiClient.setBaseUrl(context, normalized)
            ApiResult.Success(normalized)
        } else {
            ApiResult.Error(
                "Could not reach $normalized. Ensure run_backend.bat is running and both devices share Wi-Fi or Hotspot (college/public Wi-Fi may block device-to-device traffic; use Mobile Hotspot if blocked)."
            )
        }
    }

    suspend fun autoDetectServer(): ApiResult<String> = withContext(Dispatchers.IO) {
        val detected = ApiClient.autoDetectWorkingServer(context)
        if (detected != null) {
            ApiResult.Success(detected)
        } else {
            ApiResult.Error(
                "No backend found on Wi-Fi, Hotspot, USB, or Emulator. Ensure run_backend.bat is running on your PC. On college/public Wi-Fi with client isolation, connect via Phone Hotspot or Windows Mobile Hotspot."
            )
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
            is ConnectException -> "Connection refused at $currentUrl. Start the backend (run_backend.bat) or tap Auto-Connect."
            is SocketTimeoutException -> "Timed out reaching $currentUrl. Check same Wi-Fi/Hotspot, Windows Firewall (port 8000), or switch off college Wi-Fi AP isolation using Hotspot."
            is IOException -> "Cannot reach backend ($currentUrl). Tap 'Auto-Connect' or check Wi-Fi / Hotspot / USB."
            else -> throwable.localizedMessage ?: "An unexpected error occurred."
        }
    }
}
