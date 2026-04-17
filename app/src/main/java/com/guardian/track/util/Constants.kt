package com.guardian.track.util

object Constants {
    // Notification Channels
    const val CHANNEL_SURVEILLANCE = "surveillance_channel"
    const val CHANNEL_ALERTS = "alerts_channel"
    const val CHANNEL_BATTERY = "battery_channel"

    // Notification IDs
    const val NOTIFICATION_SURVEILLANCE = 1001
    const val NOTIFICATION_FALL_ALERT = 1002
    const val NOTIFICATION_BATTERY_ALERT = 1003
    const val NOTIFICATION_SMS_SIM = 1004

    // ContentProvider
    const val PROVIDER_AUTHORITY = "com.guardian.track.provider"
    const val PROVIDER_PATH_CONTACTS = "emergency_contacts"

    // Fall Detection Defaults
    const val DEFAULT_SENSITIVITY_THRESHOLD = 15.0f
    const val FREE_FALL_THRESHOLD = 3.0f
    const val FREE_FALL_DURATION_MS = 100L
    const val IMPACT_WINDOW_MS = 200L

    // WorkManager
    const val SYNC_WORK_NAME = "incident_sync_work"
    const val SERVICE_START_WORK_NAME = "service_start_work"

    // Intent Extras
    const val EXTRA_INCIDENT_TYPE = "extra_incident_type"

    // Location defaults (sentinel values when GPS denied)
    const val DEFAULT_LATITUDE = 0.0
    const val DEFAULT_LONGITUDE = 0.0
}
