package com.guardian.track.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.track.data.local.datastore.UserPreferencesRepository
import com.guardian.track.data.repository.ContactRepository
import com.guardian.track.domain.model.EmergencyContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sensitivityThreshold: Float = 15.0f,
    val isDarkMode: Boolean = false,
    val emergencyNumber: String = "",
    val isSmsSimulation: Boolean = true,
    val contacts: List<EmergencyContact> = emptyList(),
    val showAddContactDialog: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        observeContacts()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        sensitivityThreshold = prefs.sensitivityThreshold,
                        isDarkMode = prefs.isDarkMode,
                        emergencyNumber = prefs.emergencyNumber,
                        isSmsSimulation = prefs.isSmsSimulation
                    )
                }
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contactRepository.getAllContacts().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
    }

    fun updateSensitivity(threshold: Float) {
        viewModelScope.launch {
            userPreferencesRepository.updateSensitivityThreshold(threshold)
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkMode(enabled)
        }
    }

    fun updateEmergencyNumber(number: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateEmergencyNumber(number)
        }
    }

    fun updateSmsSimulation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateSmsSimulation(enabled)
        }
    }

    fun addContact(name: String, phoneNumber: String) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            _uiState.update { it.copy(message = "Name and phone number are required") }
            return
        }
        viewModelScope.launch {
            contactRepository.addContact(name, phoneNumber)
            _uiState.update {
                it.copy(showAddContactDialog = false, message = "Contact added")
            }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(message = null) }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
        }
    }

    fun showAddContactDialog() {
        _uiState.update { it.copy(showAddContactDialog = true) }
    }

    fun hideAddContactDialog() {
        _uiState.update { it.copy(showAddContactDialog = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
