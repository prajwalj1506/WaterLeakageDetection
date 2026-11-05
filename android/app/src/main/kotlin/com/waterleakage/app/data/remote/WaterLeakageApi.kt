package com.waterleakage.app.data.remote

import com.waterleakage.app.data.model.DashboardSummary
import com.waterleakage.app.data.model.DeleteResponse
import com.waterleakage.app.data.model.HealthResponse
import com.waterleakage.app.data.model.ReadingRequest
import com.waterleakage.app.data.model.ReadingResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WaterLeakageApi {

    @GET("api/v1/health")
    suspend fun checkHealth(): HealthResponse

    @POST("api/v1/readings")
    suspend fun createReading(
        @Body request: ReadingRequest
    ): ReadingResponse

    @GET("api/v1/readings")
    suspend fun getReadings(
        @Query("filter") filter: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): List<ReadingResponse>

    @GET("api/v1/readings/{id}")
    suspend fun getReadingById(
        @Path("id") id: Int
    ): ReadingResponse

    @DELETE("api/v1/readings/{id}")
    suspend fun deleteReading(
        @Path("id") id: Int
    ): DeleteResponse

    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(): DashboardSummary
}

