package com.guardian.track.domain.model

data class Incident(
    val id: Long = 0,
    val timestamp: Long,
    val type: IncidentType,
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean = false
) {
    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

    val formattedTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

    val formattedDateTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
}

enum class IncidentType {
    FALL, BATTERY, MANUAL;

    companion object {
        fun fromString(value: String): IncidentType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                MANUAL
            }
        }
    }
}
