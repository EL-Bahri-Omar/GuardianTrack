package com.guardian.track.ui.dashboard

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.track.data.local.datastore.UserPreferencesRepository
import com.guardian.track.data.repository.IncidentRepository
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.service.SurveillanceService
import com.guardian.track.util.LocationHelper
import com.guardian.track.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isServiceRunning: Boolean = false,
    val sensorMagnitude: Float = 0f,
    val batteryLevel: Int = 100,
    val isGpsEnabled: Boolean = false,
    val recentIncidents: List<Incident> = emptyList(),
    val isAlertSending: Boolean = false,
    val alertMessage: String? = null,
    val sensitivityThreshold: Float = 15f
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val incidentRepository: IncidentRepository,
    private val locationHelper: LocationHelper,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                val percentage = (level * 100) / scale
                _uiState.update { it.copy(batteryLevel = percentage) }
            }
        }
    }

    init {
        observeIncidents()
        observePreferences()
        startPollingServiceState()
        registerBatteryReceiver()
        checkGpsStatus()
    }

    private fun observeIncidents() {
        viewModelScope.launch {
            incidentRepository.getAllIncidents().collect { incidents ->
                _uiState.update { it.copy(recentIncidents = incidents.take(5)) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.sensitivityThreshold.collect { threshold ->
                _uiState.update { it.copy(sensitivityThreshold = threshold) }
            }
        }
    }

    private fun startPollingServiceState() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(
                        isServiceRunning = SurveillanceService.isRunning,
                        sensorMagnitude = SurveillanceService.lastMagnitude
                    )
                }
                delay(500) // Update every 500ms
            }
        }
    }

    private fun registerBatteryReceiver() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            getApplication<Application>().registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun checkGpsStatus() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val locationManager = getApplication<Application>()
                        .getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(
                        android.location.LocationManager.GPS_PROVIDER
                    )
                    _uiState.update { it.copy(isGpsEnabled = isGpsEnabled) }
                } catch (_: Exception) { }
                delay(2000) // Poll every 2 seconds
            }
        }
    }

    fun triggerManualAlert() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAlertSending = true) }
            try {
                val (lat, lng) = locationHelper.getCurrentLocation(getApplication())
                incidentRepository.recordIncident(
                    type = IncidentType.MANUAL,
                    latitude = lat,
                    longitude = lng
                )
                // Enqueue sync
                SyncWorker.enqueue(getApplication())

                _uiState.update {
                    it.copy(
                        isAlertSending = false,
                        alertMessage = "Manual alert recorded"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAlertSending = false,
                        alertMessage = "Failed to record alert: ${e.message}"
                    )
                }
            }

            // Clear message after delay
            delay(3000)
            _uiState.update { it.copy(alertMessage = null) }
        }
    }

    fun startService() {
        SurveillanceService.start(getApplication())
    }

    fun stopService() {
        SurveillanceService.stop(getApplication())
    }

    fun refreshGpsStatus() {
        checkGpsStatus()
    }

    fun clearAlertMessage() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
}
