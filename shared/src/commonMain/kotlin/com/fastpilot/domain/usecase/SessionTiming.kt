package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage
import kotlinx.datetime.Instant

private val CAR = Regex("""^CAR (\d+)\b""")
private val LAP = Regex("""\bLAP (\d+)\b""")

/**
 * Laps deleted by race control, as (driver number, lap number). OpenF1 has no flag on the lap
 * itself, only the message: "CAR 11 (PER) LAP DELETED - DOUBLE YELLOW AT TURN 12 LAP 2 21:32:22"
 * (2023-24: "CAR 1 (VER) TIME 1:29.708 DELETED - TRACK LIMITS AT TURN 4 LAP 2 15:26:30").
 * A later "... REINSTATED" message about the same lap brings it back.
 */
fun deletedLaps(messages: List<RaceControlMessage>): Set<Pair<Int, Int>> {
    val deleted = mutableSetOf<Pair<Int, Int>>()
    for (message in messages.sortedBy { it.date.orEmpty() }) {
        val text = message.message ?: continue
        val car = CAR.find(text)?.groupValues?.get(1)?.toInt() ?: continue
        // The last "LAP n" is the lap number ("LAP DELETED - ... LAP 2")
        val lap = LAP.findAll(text).lastOrNull()?.groupValues?.get(1)?.toInt() ?: continue
        when {
            "REINSTATED" in text -> deleted.remove(car to lap)
            "DELETED" in text -> deleted.add(car to lap)
        }
    }
    return deleted
}

/** Fastest timed lap of each driver, in seconds, leaving out the [deleted] laps. */
fun bestLapsByDriver(laps: List<Lap>, deleted: Set<Pair<Int, Int>> = emptySet()): Map<Int, Double> =
    laps.filter { it.lapDuration != null && (it.driverNumber to it.lapNumber) !in deleted }
        .groupBy { it.driverNumber }
        .mapValues { (_, driverLaps) -> driverLaps.minOf { it.lapDuration!! } }

/** Start of a qualifying phase (Q1/Q2/Q3, or SQ1/SQ2/SQ3 in a sprint qualifying). */
data class QualifyingPhase(val number: Int, val start: Instant)

/**
 * The phases of a qualifying session, in order, from race control's "SESSION STARTED"
 * messages, which carry the phase number (a restart after a red flag keeps the first start).
 */
fun qualifyingPhases(messages: List<RaceControlMessage>): List<QualifyingPhase> =
    messages.filter { it.category == "SessionStatus" && it.message == "SESSION STARTED" }
        .mapNotNull { message ->
            val number = message.qualifyingPhase ?: return@mapNotNull null
            val start = message.date?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
            QualifyingPhase(number, start)
        }
        .groupBy { it.number }
        .map { (_, starts) -> starts.minBy { it.start } }
        .sortedBy { it.number }

/** [laps] split by the phase they started in; laps without a start time are left out. */
fun lapsByPhase(laps: List<Lap>, phases: List<QualifyingPhase>): Map<Int, List<Lap>> {
    if (phases.isEmpty()) return emptyMap()
    return laps.mapNotNull { lap ->
        val start = lap.dateStart?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
        val phase = phases.lastOrNull { it.start <= start } ?: return@mapNotNull null
        phase.number to lap
    }.groupBy({ it.first }, { it.second })
}
