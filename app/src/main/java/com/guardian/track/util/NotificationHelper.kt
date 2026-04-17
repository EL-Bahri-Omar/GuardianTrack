package com.guardian.track.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guardian.track.MainActivity
import com.guardian.track.R

object NotificationHelper {

    // Notification accent colors per incident type
    private const val COLOR_SURVEILLANCE = 0xFF3B82F6.toInt() // Primary blue
    private const val COLOR_FALL = 0xFFF43F5E.toInt()          // Alert red
    private const val COLOR_BATTERY = 0xFFF59E0B.toInt()       // Warning amber
    private const val COLOR_SMS = 0xFF8B5CF6.toInt()           // Secondary purple
    private const val COLOR_MANUAL = 0xFF3B82F6.toInt()        // Primary blue

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Surveillance channel — low priority, ongoing
        val surveillanceChannel = NotificationChannel(
            Constants.CHANNEL_SURVEILLANCE,
            context.getString(R.string.notification_channel_surveillance),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing surveillance service notification"
            setShowBadge(false)
            lightColor = COLOR_SURVEILLANCE
            enableLights(true)
        }

        // Alerts channel — high priority, fall & manual alerts
        val alertsChannel = NotificationChannel(
            Constants.CHANNEL_ALERTS,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Emergency alert notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            lightColor = COLOR_FALL
            enableLights(true)
        }

        // Battery channel — high priority
        val batteryChannel = NotificationChannel(
            Constants.CHANNEL_BATTERY,
            context.getString(R.string.notification_channel_battery),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Battery critical notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
            lightColor = COLOR_BATTERY
            enableLights(true)
        }

        manager.createNotificationChannels(
            listOf(surveillanceChannel, alertsChannel, batteryChannel)
        )
    }

    fun buildSurveillanceNotification(context: Context): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.CHANNEL_SURVEILLANCE)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🛡️ GuardianTrack Active")
            .setContentText("Monitoring your safety...")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Surveillance is running. Accelerometer and location are being monitored to keep you safe."))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setColor(COLOR_SURVEILLANCE)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
    }

    fun showFallAlertNotification(context: Context, locationInfo: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("🚨 Fall Detected!")
            .setContentText("Impact detected at $locationInfo")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("🚨 Fall Detected!")
                .bigText("A sudden impact was detected at $locationInfo.\n\nEmergency contact has been notified. If you are safe, please dismiss this alert.")
                .setSummaryText("Emergency Alert"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(COLOR_FALL)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 400))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_FALL_ALERT, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun showBatteryAlertNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_BATTERY)
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setContentTitle("🔋 Battery Critical!")
            .setContentText("Battery level is critically low")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("🔋 Battery Critical!")
                .bigText("Your device battery is critically low. Safety monitoring may be interrupted. Please charge your device as soon as possible.")
                .setSummaryText("Battery Warning"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(COLOR_BATTERY)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_BATTERY_ALERT, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun showSmsSimulationNotification(context: Context, phoneNumber: String, message: String) {
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_sms)
            .setContentTitle("💬 SMS Simulated → $phoneNumber")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("💬 SMS Simulated")
                .bigText("To: $phoneNumber\n\n$message\n\n(This is a simulation — no real SMS was sent)")
                .setSummaryText("SMS Simulation"))
            .setAutoCancel(true)
            .setColor(COLOR_SMS)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_SMS_SIM, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun showManualAlertNotification(context: Context, locationInfo: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("📍 Manual Alert Sent")
            .setContentText("Alert recorded at $locationInfo")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("📍 Manual Alert Sent")
                .bigText("Your manual emergency alert was recorded at $locationInfo.\n\nEmergency contacts have been notified.")
                .setSummaryText("Manual Alert"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(COLOR_MANUAL)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_FALL_ALERT + 10, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }
}
