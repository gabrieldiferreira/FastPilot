package com.fastpilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class Interval(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("gap_to_leader") val gapToLeader: JsonPrimitive? = null,
    val interval: JsonPrimitive? = null,
    val date: String? = null,
    @SerialName("session_key") val sessionKey: Int? = null,
    @SerialName("meeting_key") val meetingKey: Int? = null
) {
    /** Gap can be a number or a string like "+1 LAP" */
    val gapToLeaderDisplay: String
        get() = gapToLeader?.let {
            if (it.isString) it.content else it.content
        } ?: "--"

    val intervalDisplay: String
        get() = interval?.let {
            if (it.isString) it.content else it.content
        } ?: "--"

    val gapToLeaderSeconds: Double?
        get() = gapToLeader?.let {
            if (!it.isString) it.content.toDoubleOrNull() else null
        }

    val intervalSeconds: Double?
        get() = interval?.let {
            if (!it.isString) it.content.toDoubleOrNull() else null
        }
}
