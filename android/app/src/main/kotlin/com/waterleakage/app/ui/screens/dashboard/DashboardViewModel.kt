package com.waterleakage.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterleakage.app.data.model.ApiResult
import com.waterleakage.app.data.model.DashboardSummary
import com.waterleakage.app.data.repository.WaterLeakageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummary? = null,
    val isServerOnline: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WaterLeakageRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Health check
            val healthRes = repository.checkHealth()
            val online = healthRes is ApiResult.Success

            // Summary metrics
            when (val summaryRes = repository.getDashboardSummary()) {
                is ApiResult.Success -> {
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        summary = summaryRes.data,
                        isServerOnline = online,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        summary = null,
                        isServerOnline = false,
                        errorMessage = summaryRes.message
                    )
                }
                else -> {}
            }
        }
    }
}

