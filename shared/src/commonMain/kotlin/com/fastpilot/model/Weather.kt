package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Weather(
    @SerialName("air_temperature") val airTemperature: Double? = null,
    @SerialName("track_temperature") val trackTemperature: Double? = null,
    val humidity: Double? = null,
    val pressure: Double? = null,
    val rainfall: Int? = null,
    @SerialName("wind_direction") val windDirection: Int? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
