package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Meeting(
    @SerialName("meeting_key") val meetingKey: Int,
    @SerialName("meeting_name") val meetingName: String? = null,
    @SerialName("meeting_official_name") val officialName: String? = null,
    @SerialName("circuit_key") val circuitKey: Int? = null,
    @SerialName("circuit_short_name") val circuitShortName: String? = null,
    @SerialName("circuit_type") val circuitType: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("country_key") val countryKey: Int? = null,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("date_end") val dateEnd: String? = null,
    @SerialName("gmt_offset") val gmtOffset: String? = null,
    val location: String? = null,
    val year: Int? = null
)
