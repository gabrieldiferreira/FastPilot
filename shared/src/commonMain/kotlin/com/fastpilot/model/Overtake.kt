package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A pass between two cars (OpenF1 /overtakes): [position] is the one gained by the overtaker. */
@Serializable
data class Overtake(
    @SerialName("overtaking_driver_number") val overtakingDriverNumber: Int,
    @SerialName("overtaken_driver_number") val overtakenDriverNumber: Int,
    val position: Int? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
