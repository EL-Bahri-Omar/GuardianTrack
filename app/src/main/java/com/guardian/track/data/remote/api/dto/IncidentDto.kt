package com.guardian.track.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class IncidentDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("timestamp")
    val timestamp: Long = 0,

    @SerializedName("type")
    val type: String = "",

    @SerializedName("latitude")
    val latitude: Double = 0.0,

    @SerializedName("longitude")
    val longitude: Double = 0.0
)
