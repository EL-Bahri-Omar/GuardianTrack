package com.guardian.track.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guardian.track.data.remote.NetworkResult
import com.guardian.track.data.repository.IncidentRepository
import com.guardian.track.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidentRepository: IncidentRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                Constants.SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            Log.d(TAG, "Sync work enqueued")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")

        return when (val result = incidentRepository.syncPendingIncidents()) {
            is NetworkResult.Success -> {
                Log.i(TAG, "Sync completed: ${result.data} incidents synced")
                Result.success()
            }
            is NetworkResult.Error -> {
                Log.e(TAG, "Sync failed: ${result.message}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
            is NetworkResult.Loading -> Result.retry()
        }
    }
}
