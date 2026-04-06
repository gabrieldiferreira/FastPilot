package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Position(
    @SerialName("driver_number") val driverNumber: Int,
    val position: Int,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
