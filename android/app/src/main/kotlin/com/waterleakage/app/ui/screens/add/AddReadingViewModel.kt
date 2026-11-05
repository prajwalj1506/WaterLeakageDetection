package com.waterleakage.app.ui.screens.add

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterleakage.app.data.model.ApiResult
import com.waterleakage.app.data.model.ReadingRequest
import com.waterleakage.app.data.model.ReadingResponse
import com.waterleakage.app.data.repository.WaterLeakageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AddReadingUiState(
    val pipeId: String = "",
    val pressure: String = "",
    val refill: Int = 0,
    val latitude: String = "12.9716",
    val longitude: String = "77.5946",
    val timestamp: String = "",
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val errorMessage: String? = null,
    val submittedReading: ReadingResponse? = null
)

class AddReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WaterLeakageRepository(application.applicationContext)
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(AddReadingUiState())
    val uiState: StateFlow<AddReadingUiState> = _uiState.asStateFlow()

    init {
        resetForm()
    }

    fun resetForm() {
        val currentTs = getCurrentIsoTimestamp()
        _uiState.value = AddReadingUiState(
            pipeId = "PIPE-${(100..999).random()}",
            pressure = "",
            refill = 0,
            latitude = "12.9716",
            longitude = "77.5946",
            timestamp = currentTs,
            isLoading = false,
            errorMessage = null,
            submittedReading = null
        )
    }

    fun onPipeIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(pipeId = value, errorMessage = null)
    }

    fun onPressureChanged(value: String) {
        _uiState.value = _uiState.value.copy(pressure = value, errorMessage = null)
    }

    fun onRefillChanged(value: Int) {
        _uiState.value = _uiState.value.copy(refill = value)
    }

    fun onLatitudeChanged(value: String) {
        _uiState.value = _uiState.value.copy(latitude = value, errorMessage = null)
    }

    fun onLongitudeChanged(value: String) {
        _uiState.value = _uiState.value.copy(longitude = value, errorMessage = null)
    }

    fun onTimestampChanged(value: String) {
        _uiState.value = _uiState.value.copy(timestamp = value)
    }

    fun updateTimestampToNow() {
        _uiState.value = _uiState.value.copy(timestamp = getCurrentIsoTimestamp())
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingLocation = true)
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = lm.getProviders(true)
                var bestLocation: Location? = null

                for (provider in providers) {
                    val l = lm.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                }

                if (bestLocation != null) {
                    _uiState.value = _uiState.value.copy(
                        latitude = String.format(Locale.US, "%.5f", bestLocation.latitude),
                        longitude = String.format(Locale.US, "%.5f", bestLocation.longitude),
                        isFetchingLocation = false
                    )
                } else {
                    // Fallback to sample coordinates if no GPS fix yet
                    _uiState.value = _uiState.value.copy(
                        latitude = "12.9716",
                        longitude = "77.5946",
                        isFetchingLocation = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetchingLocation = false,
                    errorMessage = "Could not obtain GPS location: ${e.message}"
                )
            }
        }
    }

    fun submitReading() {
        val state = _uiState.value
        val pipeId = state.pipeId.trim()
        val pressureVal = state.pressure.trim().toDoubleOrNull()
        val latVal = state.latitude.trim().toDoubleOrNull()
        val lngVal = state.longitude.trim().toDoubleOrNull()

        if (pipeId.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid Pipe ID.")
            return
        }
        if (pressureVal == null || pressureVal < 0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid numeric Pressure (e.g. 52.5).")
            return
        }
        if (latVal == null || latVal < -90.0 || latVal > 90.0) {
            _uiState.value = state.copy(errorMessage = "Latitude must be between -90 and +90.")
            return
        }
        if (lngVal == null || lngVal < -180.0 || lngVal > 180.0) {
            _uiState.value = state.copy(errorMessage = "Longitude must be between -180 and +180.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val request = ReadingRequest(
                pipeId = pipeId,
                pressure = pressureVal,
                refill = state.refill,
                latitude = latVal,
                longitude = lngVal,
                timestamp = state.timestamp.ifEmpty { getCurrentIsoTimestamp() }
            )

            when (val result = repository.submitReading(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        submittedReading = result.data,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }
}

