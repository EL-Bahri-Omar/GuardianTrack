package com.guardian.track.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.track.data.repository.IncidentRepository
import com.guardian.track.domain.model.Incident
import com.guardian.track.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val incidents: List<Incident> = emptyList(),
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val incidentRepository: IncidentRepository,
    private val csvExporter: CsvExporter
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeIncidents()
    }

    private fun observeIncidents() {
        viewModelScope.launch {
            incidentRepository.getAllIncidents().collect { incidents ->
                _uiState.update {
                    it.copy(incidents = incidents, isLoading = false)
                }
            }
        }
    }

    fun deleteIncident(id: Long) {
        viewModelScope.launch {
            incidentRepository.deleteIncident(id)
        }
    }

    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val incidents = incidentRepository.getAllIncidentsList()
                val success = csvExporter.exportIncidents(getApplication(), incidents)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = if (success) "Export successful! Check Documents/GuardianTrack/" else "Export failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = "Export failed: ${e.message}"
                    )
                }
            }

            // Clear message after delay
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(exportMessage = null) }
        }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }
}
