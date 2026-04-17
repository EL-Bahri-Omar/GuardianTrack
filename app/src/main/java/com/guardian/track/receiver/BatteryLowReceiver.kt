package com.guardian.track.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.guardian.track.data.local.db.entity.IncidentEntity
import com.guardian.track.data.local.db.GuardianDatabase
import com.guardian.track.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryLowReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryLowReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_LOW) return

        Log.w(TAG, "Battery low detected!")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create notification channels if not already created
                NotificationHelper.createNotificationChannels(context)

                // Record incident in Room
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    GuardianDatabase.DATABASE_NAME
                ).build()

                val incident = IncidentEntity(
                    timestamp = System.currentTimeMillis(),
                    type = "BATTERY",
                    latitude = 0.0,
                    longitude = 0.0,
                    isSynced = false
                )

                db.incidentDao().insertIncident(incident)
                Log.i(TAG, "Battery critical incident recorded")

                // Show notification
                NotificationHelper.showBatteryAlertNotification(context)

            } catch (e: Exception) {
                Log.e(TAG, "Error handling battery low event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
