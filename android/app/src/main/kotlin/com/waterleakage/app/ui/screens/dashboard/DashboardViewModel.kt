package com.waterleakage.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterleakage.app.data.model.ApiResult
import com.waterleakage.app.data.model.DashboardSummary
import com.waterleakage.app.data.remote.ApiClient
import com.waterleakage.app.data.repository.WaterLeakageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummary? = null,
    val isServerOnline: Boolean = false,
    val activeUrl: String = "",
    val connectionType: String = "",
    val isAutoConnecting: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository = WaterLeakageRepository(context)

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            activeUrl = ApiClient.getBaseUrl(context),
            connectionType = ApiClient.getConnectionLabel(ApiClient.getBaseUrl(context))
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                activeUrl = ApiClient.getBaseUrl(context),
                connectionType = ApiClient.getConnectionLabel(ApiClient.getBaseUrl(context))
            )

            // Health check with auto-fallback
            val healthRes = repository.checkHealth()
            val online = healthRes is ApiResult.Success

            val currentUrl = ApiClient.getBaseUrl(context)
            val currentLabel = ApiClient.getConnectionLabel(currentUrl)

            if (online) {
                when (val summaryRes = repository.getDashboardSummary()) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            summary = summaryRes.data,
                            isServerOnline = true,
                            activeUrl = currentUrl,
                            connectionType = currentLabel,
                            errorMessage = null
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            summary = null,
                            isServerOnline = true,
                            activeUrl = currentUrl,
                            connectionType = currentLabel,
                            errorMessage = summaryRes.message
                        )
                    }
                    else -> {}
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = null,
                    isServerOnline = false,
                    activeUrl = currentUrl,
                    connectionType = currentLabel,
                    errorMessage = (healthRes as? ApiResult.Error)?.message
                )
            }
        }
    }

    fun autoConnect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoConnecting = true, errorMessage = null)
            when (val res = repository.autoDetectServer()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isAutoConnecting = false)
                    loadDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAutoConnecting = false,
                        errorMessage = res.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isAutoConnecting = false)
                }
            }
        }
    }
}
