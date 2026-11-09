package com.waterleakage.app.data.remote

import android.content.Context
import android.os.Build
import com.waterleakage.app.data.model.HealthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val PREFS_NAME = "water_leakage_prefs"
    private const val KEY_BASE_URL = "base_url"

    // Default endpoints
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000/"
    const val USB_BASE_URL = "http://127.0.0.1:8000/"
    const val WIFI_BASE_URL = "http://192.168.29.251:8000/"

    // Fast-ping HTTP client for zero-lag server probing (3s timeout instead of 15s)
    private val fastPingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    private var currentBaseUrl: String = getDefaultBaseUrl()
    private var cachedApi: WaterLeakageApi? = null

    fun isRunningOnEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    fun getDefaultBaseUrl(): String {
        return if (isRunningOnEmulator()) EMULATOR_BASE_URL else USB_BASE_URL
    }

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, getDefaultBaseUrl()) ?: getDefaultBaseUrl()
    }

    fun setBaseUrl(context: Context, newUrl: String) {
        var formatted = newUrl.trim()
        if (!formatted.endsWith("/")) {
            formatted += "/"
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        currentBaseUrl = formatted
        cachedApi = null // Force recreation on next getApi()
    }

    fun getConnectionLabel(url: String): String {
        return when {
            url.contains("127.0.0.1") || url.contains("localhost") -> "USB Cable"
            url.contains("10.0.2.2") -> "Emulator"
            url.contains("192.168.") || url.contains("10.") -> "Wi-Fi"
            else -> "Custom"
        }
    }

    /**
     * Probes candidate server endpoints in priority order and auto-selects the first
     * reachable, healthy backend. Saves the winning URL to SharedPreferences.
     */
    suspend fun autoDetectWorkingServer(context: Context): String? = withContext(Dispatchers.IO) {
        val savedUrl = getBaseUrl(context)
        val candidates = linkedSetOf(
            savedUrl,
            USB_BASE_URL,
            "http://localhost:8000/",
            WIFI_BASE_URL,
            EMULATOR_BASE_URL
        )

        for (candidate in candidates) {
            val normalized = if (candidate.endsWith("/")) candidate else "$candidate/"
            val healthUrl = "${normalized}api/v1/health"
            try {
                val request = Request.Builder().url(healthUrl).get().build()
                val response = fastPingClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.contains("\"healthy\"")) {
                        // Found healthy backend! Save and switch immediately.
                        setBaseUrl(context, normalized)
                        return@withContext normalized
                    }
                }
            } catch (_: Exception) {
                // Endpoint unreachable; continue to next candidate
            }
        }
        null
    }

    fun getApi(context: Context): WaterLeakageApi {
        val configuredUrl = getBaseUrl(context)
        if (cachedApi == null || currentBaseUrl != configuredUrl) {
            currentBaseUrl = configuredUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Balanced timeouts: 8s connect, 12s read/write for actual ML inferences
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            cachedApi = retrofit.create(WaterLeakageApi::class.java)
        }
        return cachedApi!!
    }
}
