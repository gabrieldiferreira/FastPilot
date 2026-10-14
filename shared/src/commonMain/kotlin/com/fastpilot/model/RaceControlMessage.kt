package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RaceControlMessage(
    val category: String? = null,
    val flag: String? = null,
    val message: String? = null,
    @SerialName("driver_number") val driverNumber: Int? = null,
    @SerialName("lap_number") val lapNumber: Int? = null,
    val scope: String? = null,
    val sector: Int? = null,
    @SerialName("qualifying_phase") val qualifyingPhase: Int? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
