package com.guardian.track.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_preferences")

data class UserPreferencesData(
    val sensitivityThreshold: Float = 15.0f,
    val isDarkMode: Boolean = false,
    val emergencyNumber: String = "",
    val isSmsSimulation: Boolean = false
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object PreferencesKeys {
        val SENSITIVITY_THRESHOLD = floatPreferencesKey("sensitivity_threshold")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val EMERGENCY_NUMBER = stringPreferencesKey("emergency_number")
        val SMS_SIMULATION = booleanPreferencesKey("sms_simulation")
    }

    val userPreferences: Flow<UserPreferencesData> = dataStore.data.map { preferences ->
        UserPreferencesData(
            sensitivityThreshold = preferences[PreferencesKeys.SENSITIVITY_THRESHOLD] ?: 15.0f,
            isDarkMode = preferences[PreferencesKeys.DARK_MODE] ?: false,
            emergencyNumber = preferences[PreferencesKeys.EMERGENCY_NUMBER] ?: "",
            isSmsSimulation = preferences[PreferencesKeys.SMS_SIMULATION] ?: false
        )
    }

    val sensitivityThreshold: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SENSITIVITY_THRESHOLD] ?: 15.0f
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: false
    }

    val isSmsSimulation: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMS_SIMULATION] ?: false
    }

    val emergencyNumber: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EMERGENCY_NUMBER] ?: ""
    }

    suspend fun updateSensitivityThreshold(threshold: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SENSITIVITY_THRESHOLD] = threshold
        }
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun updateEmergencyNumber(number: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMERGENCY_NUMBER] = number
        }
    }

    suspend fun updateSmsSimulation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMS_SIMULATION] = enabled
        }
    }
}
