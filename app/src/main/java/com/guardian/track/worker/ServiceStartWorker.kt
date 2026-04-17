package com.guardian.track.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guardian.track.service.SurveillanceService
import com.guardian.track.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ServiceStartWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ServiceStartWorker"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ServiceStartWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                Constants.SERVICE_START_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )

            Log.d(TAG, "Service start work enqueued (expedited)")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting SurveillanceService from WorkManager")

        return try {
            SurveillanceService.start(applicationContext)
            Log.i(TAG, "SurveillanceService started successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SurveillanceService", e)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notification = com.guardian.track.util.NotificationHelper
            .buildSurveillanceNotification(applicationContext)
            .build()
        return androidx.work.ForegroundInfo(
            Constants.NOTIFICATION_SURVEILLANCE,
            notification
        )
    }
}
