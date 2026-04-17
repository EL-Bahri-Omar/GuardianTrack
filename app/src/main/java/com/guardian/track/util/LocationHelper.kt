package com.guardian.track.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationHelper @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {

    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_TIMEOUT_MS = 5000L
        private const val MAX_LOCATION_AGE_MS = 30_000L
    }

    suspend fun getCurrentLocation(context: Context): Pair<Double, Double> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted, returning sentinel values")
            return Pair(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)
        }

        return try {
            // Always try fresh location FIRST (high accuracy GPS)
            val freshLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                getCurrentLocationFresh(context)
            }

            if (freshLocation != null) {
                Log.d(TAG, "Fresh location obtained: ${freshLocation.latitude}, ${freshLocation.longitude}")
                return Pair(freshLocation.latitude, freshLocation.longitude)
            }

            // Fallback: use last known location only if recent
            val lastKnown = getLastKnownLocation()
            if (lastKnown != null) {
                val age = System.currentTimeMillis() - lastKnown.time
                if (age <= MAX_LOCATION_AGE_MS) {
                    Log.d(TAG, "Using recent last known location (age: ${age}ms)")
                    return Pair(lastKnown.latitude, lastKnown.longitude)
                } else {
                    Log.w(TAG, "Last known location too old (age: ${age}ms), discarding")
                }
            }

            Log.w(TAG, "Unable to get location, returning sentinel values")
            Pair(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            Pair(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)
        }
    }

    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }

    private suspend fun getCurrentLocationFresh(context: Context): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener {
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }
}
