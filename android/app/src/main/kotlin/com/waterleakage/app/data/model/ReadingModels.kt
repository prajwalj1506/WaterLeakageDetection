package com.waterleakage.app.data.model

import com.google.gson.annotations.SerializedName

data class ReadingRequest(
    @SerializedName("pipe_id") val pipeId: String,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("refill") val refill: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: String
)

data class ReadingResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("pipe_id") val pipeId: String,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("refill") val refill: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("reading_timestamp") val readingTimestamp: String,
    @SerializedName("prob_logistic_regression") val probLogisticRegression: Double,
    @SerializedName("prob_decision_tree") val probDecisionTree: Double,
    @SerializedName("prob_random_forest") val probRandomForest: Double,
    @SerializedName("prob_svm") val probSvm: Double,
    @SerializedName("pred_logistic_regression") val predLogisticRegression: String,
    @SerializedName("pred_decision_tree") val predDecisionTree: String,
    @SerializedName("pred_random_forest") val predRandomForest: String,
    @SerializedName("pred_svm") val predSvm: String,
    @SerializedName("average_probability") val averageProbability: Double,
    @SerializedName("final_prediction") val finalPrediction: String,
    @SerializedName("created_at") val createdAt: String
)

data class DashboardSummary(
    @SerializedName("total_readings") val totalReadings: Int,
    @SerializedName("leaks_detected") val leaksDetected: Int,
    @SerializedName("normal_readings") val normalReadings: Int,
    @SerializedName("leak_rate_percentage") val leakRatePercentage: Double,
    @SerializedName("recent_readings") val recentReadings: List<ReadingResponse>
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String,
    @SerializedName("models_loaded") val modelsLoaded: Boolean,
    @SerializedName("database_connected") val databaseConnected: Boolean,
    @SerializedName("timestamp") val timestamp: String
)

data class DeleteResponse(
    @SerializedName("message") val message: String,
    @SerializedName("id") val id: Int
)

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

