package com.guardian.track.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SecureStorage"
        private const val FILE_NAME = "guardian_secure_prefs"
        private const val KEY_EMERGENCY_NUMBER = "secure_emergency_number"
        private const val KEY_API_KEY = "secure_api_key"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back", e)
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveEmergencyNumber(number: String) {
        sharedPreferences.edit().putString(KEY_EMERGENCY_NUMBER, number).apply()
    }

    fun getEmergencyNumber(): String {
        return sharedPreferences.getString(KEY_EMERGENCY_NUMBER, "") ?: ""
    }

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
