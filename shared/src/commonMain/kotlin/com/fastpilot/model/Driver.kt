package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Driver(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("broadcast_name") val broadcastName: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("name_acronym") val nameAcronym: String? = null,
    @SerialName("team_name") val teamName: String? = null,
    @SerialName("team_colour") val teamColour: String? = null,
    @SerialName("headshot_url") val headshotUrl: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
