package com.waterleakage.app.ui.screens.details

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

data class DetailUiState(
    val isLoading: Boolean = true,
    val reading: ReadingResponse? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class ReadingDetailViewModel(
    application: Application,
    private val readingId: Int
) : AndroidViewModel(application) {
    private val repository = WaterLeakageRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            when (val res = repository.getReadingById(readingId)) {
                is ApiResult.Success -> {
                    _uiState.value = DetailUiState(
                        isLoading = false,
                        reading = res.data,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = DetailUiState(
                        isLoading = false,
                        reading = null,
                        errorMessage = res.message
                    )
                }
                else -> {}
            }
        }
    }

    fun deleteReading() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            when (val res = repository.deleteReading(readingId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        isDeleted = true
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = res.message
                    )
                }
                else -> {}
            }
        }
    }
}

