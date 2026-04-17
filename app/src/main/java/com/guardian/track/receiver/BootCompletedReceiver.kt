package com.guardian.track.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.guardian.track.service.SurveillanceService
import com.guardian.track.worker.ServiceStartWorker

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot completed, restarting surveillance service")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ : Use WorkManager with setExpedited() as fallback
            Log.d(TAG, "API 31+ detected, using WorkManager to start service")
            ServiceStartWorker.enqueue(context)
        } else {
            // API < 31 : Start service directly
            try {
                SurveillanceService.start(context)
                Log.d(TAG, "Service started directly (API < 31)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service directly, falling back to WorkManager", e)
                ServiceStartWorker.enqueue(context)
            }
        }
    }
}
