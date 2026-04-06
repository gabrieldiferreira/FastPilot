package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lap(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("lap_number") val lapNumber: Int,
    @SerialName("lap_duration") val lapDuration: Double? = null,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("duration_sector_1") val sector1: Double? = null,
    @SerialName("duration_sector_2") val sector2: Double? = null,
    @SerialName("duration_sector_3") val sector3: Double? = null,
    @SerialName("i1_speed") val i1Speed: Int? = null,
    @SerialName("i2_speed") val i2Speed: Int? = null,
    @SerialName("st_speed") val stSpeed: Int? = null,
    @SerialName("is_pit_out_lap") val isPitOutLap: Boolean = false,
    @SerialName("segments_sector_1") val segmentsSector1: List<Int?>? = null,
    @SerialName("segments_sector_2") val segmentsSector2: List<Int?>? = null,
    @SerialName("segments_sector_3") val segmentsSector3: List<Int?>? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
)
