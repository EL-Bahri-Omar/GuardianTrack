package com.guardian.track.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.HandlerThread
import android.os.IBinder
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.guardian.track.data.local.datastore.UserPreferencesRepository
import com.guardian.track.data.repository.IncidentRepository
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.util.Constants
import com.guardian.track.util.LocationHelper
import com.guardian.track.util.NotificationHelper
import com.guardian.track.util.SmsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SurveillanceService : Service(), SensorEventListener, FallDetector.OnFallDetectedListener {

    companion object {
        private const val TAG = "SurveillanceService"

        // Shared state for UI observation
        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var lastMagnitude: Float = 0f
            private set

        fun start(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java)
            context.stopService(intent)
        }
    }

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var locationHelper: LocationHelper
    @Inject lateinit var smsHelper: SmsHelper
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var sensorHandlerThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val fallDetector = FallDetector()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        NotificationHelper.createNotificationChannels(this)

        // Setup Fall Detector
        fallDetector.setListener(this)

        // Observe threshold changes
        serviceScope.launch {
            userPreferencesRepository.sensitivityThreshold.collect { threshold ->
                fallDetector.updateThreshold(threshold)
            }
        }

        // Setup sensor on dedicated HandlerThread
        sensorHandlerThread = HandlerThread("SensorThread").also {
            it.start()
            sensorHandler = Handler(it.looper)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Start as foreground service
        val notification = NotificationHelper.buildSurveillanceNotification(this).build()
        startForeground(Constants.NOTIFICATION_SURVEILLANCE, notification)

        // Register accelerometer listener on dedicated thread
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME, // ~20ms
                sensorHandler
            )
        } ?: Log.w(TAG, "No accelerometer sensor available")

        // Acquire partial wake lock to keep sensor alive
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GuardianTrack::SurveillanceWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes max
        }

        isRunning = true

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val ax = it.values[0]
                val ay = it.values[1]
                val az = it.values[2]

                lastMagnitude = fallDetector.getCurrentMagnitude(ax, ay, az)
                fallDetector.processSensorData(ax, ay, az)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    override fun onFallDetected(magnitude: Float) {
        Log.w(TAG, "Fall detected with magnitude: $magnitude m/s²")

        serviceScope.launch {
            try {
                // Get current location
                val (latitude, longitude) = locationHelper.getCurrentLocation(this@SurveillanceService)

                // Record incident in Room
                val id = incidentRepository.recordIncident(
                    type = IncidentType.FALL,
                    latitude = latitude,
                    longitude = longitude
                )

                // Show notification
                val locationInfo = if (latitude != 0.0 || longitude != 0.0) {
                    "Lat: %.7f, Lng: %.7f".format(latitude, longitude)
                } else {
                    "Location unavailable"
                }
                NotificationHelper.showFallAlertNotification(this@SurveillanceService, locationInfo)

                // Send emergency SMS
                val message = "⚠️ GUARDIAN TRACK ALERT: Fall detected! " +
                        "Location: $locationInfo. " +
                        "Time: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                smsHelper.sendEmergencySms(this@SurveillanceService, message)

                Log.i(TAG, "Fall incident recorded with id: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fall detection", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        isRunning = false
        lastMagnitude = 0f

        // Unregister sensor
        sensorManager?.unregisterListener(this)

        // Stop handler thread
        sensorHandlerThread?.quitSafely()
        sensorHandlerThread = null
        sensorHandler = null

        // Release wake lock
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        // Cancel coroutines
        serviceScope.cancel()

        fallDetector.reset()
    }
}
