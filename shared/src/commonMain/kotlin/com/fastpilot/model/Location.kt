package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Location(
    @SerialName("driver_number") val driverNumber: Int,
    val x: Int? = null,
    val y: Int? = null,
    val z: Int? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
