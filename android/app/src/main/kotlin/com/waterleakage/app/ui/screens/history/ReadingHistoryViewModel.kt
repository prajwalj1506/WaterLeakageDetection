package com.waterleakage.app.ui.screens.history

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

enum class HistoryFilter { ALL, LEAKS, NORMAL }

data class HistoryUiState(
    val isLoading: Boolean = true,
    val readings: List<ReadingResponse> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class ReadingHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WaterLeakageRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadReadings()
    }

    fun loadReadings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val filterParam = when (_uiState.value.filter) {
                HistoryFilter.ALL -> null
                HistoryFilter.LEAKS -> "Leak"
                HistoryFilter.NORMAL -> "No Leak"
            }
            val searchParam = _uiState.value.searchQuery.ifBlank { null }

            when (val res = repository.getReadings(filter = filterParam, search = searchParam)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        readings = res.data,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        readings = emptyList(),
                        errorMessage = res.message
                    )
                }
                else -> {}
            }
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadReadings()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadReadings()
    }
}

