package com.waterleakage.app

import com.google.gson.Gson
import com.waterleakage.app.data.model.ReadingRequest
import com.waterleakage.app.data.model.ReadingResponse
import org.junit.Assert.*
import org.junit.Test

class DataModelTest {

    private val gson = Gson()

    @Test
    fun readingRequest_serialization() {
        val request = ReadingRequest(
            pipeId = "PIPE-202",
            pressure = 48.5,
            refill = 0,
            latitude = 12.9716,
            longitude = 77.5946,
            timestamp = "2026-11-05T22:00:00"
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("\"pipe_id\":\"PIPE-202\""))
        assertTrue(json.contains("\"pressure\":48.5"))
        assertTrue(json.contains("\"refill\":0"))
    }

    @Test
    fun readingResponse_deserialization() {
        val json = """
            {
                "id": 42,
                "pipe_id": "PIPE-999",
                "pressure": 32.1,
                "refill": 1,
                "latitude": 13.0,
                "longitude": 78.0,
                "reading_timestamp": "2026-11-05T12:00:00",
                "prob_logistic_regression": 0.85,
                "prob_decision_tree": 0.90,
                "prob_random_forest": 0.88,
                "prob_svm": 0.92,
                "pred_logistic_regression": "Leak",
                "pred_decision_tree": "Leak",
                "pred_random_forest": "Leak",
                "pred_svm": "Leak",
                "average_probability": 0.8875,
                "final_prediction": "Leak",
                "created_at": "2026-11-05T12:00:01"
            }
        """.trimIndent()

        val response = gson.fromJson(json, ReadingResponse::class.java)
        assertEquals(42, response.id)
        assertEquals("PIPE-999", response.pipeId)
        assertEquals(32.1, response.pressure, 0.001)
        assertEquals("Leak", response.finalPrediction)
        assertEquals(0.8875, response.averageProbability, 0.0001)
    }

    @Test
    fun leakThreshold_check() {
        val leakProb = 0.50
        val noLeakProb = 0.4999
        assertTrue("Prob >= 0.5 should be Leak", leakProb >= 0.5)
        assertFalse("Prob < 0.5 should be No Leak", noLeakProb >= 0.5)
    }
}

