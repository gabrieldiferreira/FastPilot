package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTimingTest {

    private fun rc(date: String, message: String, category: String = "Other", phase: Int? = null) =
        RaceControlMessage(category = category, message = message, date = date, qualifyingPhase = phase)

    // Singapore 2026 qualifying (session 11384), trimmed
    private val messages = listOf(
        rc("2026-10-10T13:30:00.122000+00:00", "SESSION STARTED", "SessionStatus", 1),
        rc("2026-10-10T13:33:51+00:00", "CAR 11 (PER) LAP DELETED - DOUBLE YELLOW AT TURN 12 LAP 2 21:32:22"),
        rc("2026-10-10T13:36:22+00:00", "CAR 41 (LIN) LAP DELETED - TRACK LIMITS AT TURN 5 LAP 4 21:34:30 (PIT)"),
        rc("2026-10-10T13:40:00+00:00", "CAR 1 (VER) TIME 1:29.708 DELETED - TRACK LIMITS AT TURN 4 LAP 3 15:26:30"),
        rc("2026-10-10T13:45:00+00:00", "CAR 41 (LIN) LAP 4 TIME 1:32.100 REINSTATED"),
        rc("2026-10-10T13:48:00.100000+00:00", "SESSION FINISHED", "SessionStatus", 1),
        rc("2026-10-10T13:55:00.116000+00:00", "SESSION STARTED", "SessionStatus", 2),
        rc("2026-10-10T14:17:00.106000+00:00", "SESSION STARTED", "SessionStatus", 3),
        rc("2026-10-10T14:20:00+00:00", "SESSION STARTED", "SessionStatus", 3), // restart after a red flag
    )

    private fun lap(driver: Int, number: Int, duration: Double?, start: String?) =
        Lap(driverNumber = driver, lapNumber = number, lapDuration = duration, dateStart = start)

    @Test
    fun raceControlDecodesTheQualifyingPhaseOpenF1SendsAsANumber() {
        // A String field made the whole qualifying race control list fail to decode
        val json = """[{"date":"2026-10-10T13:30:00.122000+00:00","category":"SessionStatus","qualifying_phase":1,
            "message":"SESSION STARTED","lap_number":null,"driver_number":null}]"""
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(ListSerializer(RaceControlMessage.serializer()), json)
        assertEquals(listOf(1), qualifyingPhases(decoded).map { it.number })
    }

    @Test
    fun deletedLapsHandleBothMessageFormatsAndReinstatements() {
        assertEquals(setOf(11 to 2, 1 to 3), deletedLaps(messages))
    }

    @Test
    fun bestLapSkipsDeletedAndUntimedLaps() {
        val laps = listOf(
            lap(11, 1, null, null),
            lap(11, 2, 90.0, null),
            lap(11, 3, 91.5, null),
            lap(16, 2, 92.5, null),
        )
        assertEquals(mapOf(11 to 91.5, 16 to 92.5), bestLapsByDriver(laps, deletedLaps(messages)))
        assertEquals(mapOf(11 to 90.0, 16 to 92.5), bestLapsByDriver(laps))
    }

    @Test
    fun lapsAreSplitByTheQualifyingPhaseTheyStartedIn() {
        val phases = qualifyingPhases(messages)
        assertEquals(listOf(1, 2, 3), phases.map { it.number })
        val laps = listOf(
            lap(1, 1, null, null),
            lap(1, 2, 92.0, "2026-10-10T13:31:00.000000+00:00"),
            lap(1, 3, 91.9, "2026-10-10T13:47:59+00:00"), // started before the chequered flag
            lap(1, 4, 91.5, "2026-10-10T14:00:00+00:00"),
            lap(1, 5, 91.3, "2026-10-10T14:18:00+00:00"),
        )
        val byPhase = lapsByPhase(laps, phases)
        assertEquals(listOf(2, 3), byPhase[1]?.map { it.lapNumber })
        assertEquals(listOf(4), byPhase[2]?.map { it.lapNumber })
        assertEquals(listOf(5), byPhase[3]?.map { it.lapNumber })
    }
}
