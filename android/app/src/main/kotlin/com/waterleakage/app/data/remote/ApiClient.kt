package com.waterleakage.app.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val PREFS_NAME = "water_leakage_prefs"
    private const val KEY_BASE_URL = "base_url"

    // Default for Android Emulator (maps to localhost:8000 on host PC)
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var cachedApi: WaterLeakageApi? = null

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
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

    fun getApi(context: Context): WaterLeakageApi {
        val configuredUrl = getBaseUrl(context)
        if (cachedApi == null || currentBaseUrl != configuredUrl) {
            currentBaseUrl = configuredUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
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

