package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarData(
    @SerialName("driver_number") val driverNumber: Int,
    val speed: Int? = null,
    val throttle: Int? = null,
    val brake: Int? = null,
    val rpm: Int? = null,
    @SerialName("n_gear") val gear: Int? = null,
    val drs: Int? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
