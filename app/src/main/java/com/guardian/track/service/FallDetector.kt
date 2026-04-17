package com.guardian.track.service

import android.util.Log
import com.guardian.track.util.Constants

class FallDetector(
    private var impactThreshold: Float = Constants.DEFAULT_SENSITIVITY_THRESHOLD
) {

    companion object {
        private const val TAG = "FallDetector"
    }

    interface OnFallDetectedListener {
        fun onFallDetected(magnitude: Float)
    }

    private var listener: OnFallDetectedListener? = null
    private var isInFreeFall = false
    private var freeFallStartTime: Long = 0L
    private var freeFallConfirmed = false
    private var impactWindowStartTime: Long = 0L

    fun setListener(listener: OnFallDetectedListener) {
        this.listener = listener
    }

    fun updateThreshold(threshold: Float) {
        this.impactThreshold = threshold
        Log.d(TAG, "Impact threshold updated to: $threshold m/s²")
    }

    fun processSensorData(ax: Float, ay: Float, az: Float) {
        val magnitude = Math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        val currentTime = System.currentTimeMillis()

        when {
            // Phase 1: Detect free-fall
            !freeFallConfirmed && magnitude < Constants.FREE_FALL_THRESHOLD -> {
                if (!isInFreeFall) {
                    isInFreeFall = true
                    freeFallStartTime = currentTime
                    Log.d(TAG, "Free-fall phase started, magnitude: $magnitude")
                } else if (currentTime - freeFallStartTime > Constants.FREE_FALL_DURATION_MS) {
                    // Free-fall confirmed (> 100ms)
                    freeFallConfirmed = true
                    impactWindowStartTime = currentTime
                    Log.d(TAG, "Free-fall confirmed after ${currentTime - freeFallStartTime}ms")
                }
            }

            // Phase 2: Detect impact after confirmed free-fall
            freeFallConfirmed -> {
                val timeSinceFreeFall = currentTime - impactWindowStartTime

                if (timeSinceFreeFall <= Constants.IMPACT_WINDOW_MS) {
                    if (magnitude > impactThreshold) {
                        // Impact detected!
                        Log.w(TAG, "FALL DETECTED! Impact magnitude: $magnitude m/s² (threshold: $impactThreshold)")
                        listener?.onFallDetected(magnitude)
                        reset()
                    }
                } else {
                    // Impact window expired without detection
                    Log.d(TAG, "Impact window expired, resetting")
                    reset()
                }
            }

            // No free-fall condition - reset if we were tracking
            else -> {
                if (isInFreeFall) {
                    Log.d(TAG, "Free-fall interrupted, magnitude: $magnitude")
                    reset()
                }
            }
        }
    }

    fun reset() {
        isInFreeFall = false
        freeFallStartTime = 0L
        freeFallConfirmed = false
        impactWindowStartTime = 0L
    }

    fun getCurrentMagnitude(ax: Float, ay: Float, az: Float): Float {
        return Math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
    }
}
