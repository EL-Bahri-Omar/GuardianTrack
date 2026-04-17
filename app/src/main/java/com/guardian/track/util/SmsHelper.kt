package com.guardian.track.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.guardian.track.data.local.datastore.UserPreferencesRepository
import com.guardian.track.data.repository.ContactRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsHelper @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val contactRepository: ContactRepository
) {

    companion object {
        private const val TAG = "SmsHelper"
    }

    /**
     * Sends emergency SMS to ALL configured contacts:
     * 1. The main emergency number from preferences
     * 2. All emergency contacts stored in Room database
     *
     * SMS is sent as a REAL text message via SmsManager and also
     * written to the SMS content provider so it appears in the
     * device's Messages app (sent folder).
     *
     * If SMS Simulation mode is ON, shows a notification instead.
     */
    suspend fun sendEmergencySms(context: Context, message: String) {
        val prefs = userPreferencesRepository.userPreferences.first()

        // Collect all phone numbers to notify
        val phoneNumbers = mutableSetOf<String>()

        // 1. Main emergency number from preferences
        if (prefs.emergencyNumber.isNotBlank()) {
            phoneNumbers.add(prefs.emergencyNumber)
        }

        // 2. All emergency contacts from Room database
        val contacts = contactRepository.getAllContacts().first()
        contacts.forEach { contact ->
            if (contact.phoneNumber.isNotBlank()) {
                phoneNumbers.add(contact.phoneNumber)
            }
        }

        if (phoneNumbers.isEmpty()) {
            Log.w(TAG, "No emergency contacts configured — SMS not sent")
            return
        }

        if (prefs.isSmsSimulation) {
            // Simulation mode (toggle OFF): no SMS, no notification
            Log.i(TAG, "[SMS DISABLED] SMS Alerts toggle is OFF — skipping SMS")
            return
        } else {
            // Real SMS mode (toggle ON): send SMS + show notification
            phoneNumbers.forEach { phoneNumber ->
                sendRealSms(context, phoneNumber, message)
                NotificationHelper.showSmsSimulationNotification(context, phoneNumber, message)
            }
        }

        Log.i(TAG, "Emergency SMS sent to ${phoneNumbers.size} contact(s): $phoneNumbers")
    }

    private fun sendRealSms(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "SEND_SMS permission not granted")
            return
        }

        try {
            @Suppress("DEPRECATION")
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }

            // Split long messages into parts and send
            val parts = smsManager?.divideMessage(message)
            if (parts != null && parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager?.sendTextMessage(phoneNumber, null, message, null, null)
            }
            Log.i(TAG, "SMS sent to $phoneNumber")

            // Write SMS to the sent folder so it appears in the Messages app
            writeSmsToSentFolder(context, phoneNumber, message)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
        }
    }

    private fun writeSmsToSentFolder(context: Context, phoneNumber: String, message: String) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, phoneNumber)
                put(Telephony.Sms.BODY, message)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            Log.i(TAG, "SMS written to sent folder for $phoneNumber")
        } catch (e: Exception) {
            // Not critical — SMS was still sent, just won't show in Messages app
            Log.w(TAG, "Could not write SMS to sent folder: ${e.message}")
        }
    }
}
