package com.waterleakage.app.ui.screens.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterleakage.app.data.model.ApiResult
import com.waterleakage.app.data.model.ReadingResponse
import com.waterleakage.app.data.repository.WaterLeakageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PredictionResultUiState(
    val isLoading: Boolean = true,
    val reading: ReadingResponse? = null,
    val errorMessage: String? = null
)

class PredictionResultViewModel(
    application: Application,
    private val readingId: Int
) : AndroidViewModel(application) {
    private val repository = WaterLeakageRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(PredictionResultUiState())
    val uiState: StateFlow<PredictionResultUiState> = _uiState.asStateFlow()

    init {
        loadReading()
    }

    fun loadReading() {
        viewModelScope.launch {
            _uiState.value = PredictionResultUiState(isLoading = true)
            when (val res = repository.getReadingById(readingId)) {
                is ApiResult.Success -> {
                    _uiState.value = PredictionResultUiState(
                        isLoading = false,
                        reading = res.data,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = PredictionResultUiState(
                        isLoading = false,
                        reading = null,
                        errorMessage = res.message
                    )
                }
                else -> {}
            }
        }
    }
}

