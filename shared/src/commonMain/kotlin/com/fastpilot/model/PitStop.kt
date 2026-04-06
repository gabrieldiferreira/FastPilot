package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PitStop(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("lap_number") val lapNumber: Int? = null,
    @SerialName("pit_duration") val pitDuration: Double? = null,
    @SerialName("stop_duration") val stopDuration: Double? = null,
    @SerialName("lane_duration") val laneDuration: Double? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
