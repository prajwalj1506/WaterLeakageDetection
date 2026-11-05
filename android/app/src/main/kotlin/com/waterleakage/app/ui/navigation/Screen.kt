package com.waterleakage.app.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddReading : Screen("add_reading")
    object ReadingHistory : Screen("reading_history")

    object PredictionResult : Screen("prediction_result/{readingId}") {
        fun createRoute(readingId: Int) = "prediction_result/$readingId"
    }

    object ReadingDetails : Screen("reading_details/{readingId}") {
        fun createRoute(readingId: Int) = "reading_details/$readingId"
    }
}

