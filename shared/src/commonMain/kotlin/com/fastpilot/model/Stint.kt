package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Stint(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("stint_number") val stintNumber: Int,
    val compound: String? = null,
    @SerialName("lap_start") val lapStart: Int? = null,
    @SerialName("lap_end") val lapEnd: Int? = null,
    @SerialName("tyre_age_at_start") val tyreAgeAtStart: Int? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
