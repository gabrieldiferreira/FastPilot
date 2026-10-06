package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Lap filters shared by the clean pace and tyre degradation metrics (TCC I §4.7):
 * atypical laps are the standing-start lap, pit in/out laps, the lap on which a car
 * retires and laps run under safety car, virtual safety car or red flag.
 */
object LapFilters {

    /** A neutralised period in session time. */
    data class Period(val start: Instant, val end: Instant)

    /** Time between "VIRTUAL SAFETY CAR ENDING" and the return to racing speed. */
    private val VSC_ENDING_COUNTDOWN = 15.seconds

    /**
     * Safety car, virtual safety car and red flag periods rebuilt from /race_control.
     *
     * Race control numbers messages with the leader's lap, so a lapped car is on a different
     * lap at the same moment: periods are therefore kept as time intervals. A period starts at
     * "... SAFETY CAR DEPLOYED" or "RED FLAG" and ends when the leader completes the
     * "SAFETY CAR IN THIS LAP" lap, 15 s after "VIRTUAL SAFETY CAR ENDING", or when the leader
     * completes the lap after a red flag (restart lap). [laps] (all drivers) give the leader's timing.
     */
    fun neutralisedPeriods(messages: List<RaceControlMessage>, laps: List<Lap>): List<Period> {
        // When the leader (first car to do so) starts each lap
        val leaderLapStart: Map<Int, Instant> = laps
            .mapNotNull { lap -> lap.dateStart?.let { lap.lapNumber to Instant.parse(it) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, starts) -> starts.min() }
        fun leaderCompletes(lap: Int, fallback: Instant) = leaderLapStart[lap + 1] ?: (fallback + 2.seconds * 60)

        val periods = mutableListOf<Period>()
        var openedAt: Instant? = null
        for (msg in messages.filter { it.date != null }.sortedBy { it.date }) {
            val time = Instant.parse(msg.date!!)
            val lap = msg.lapNumber ?: continue
            val text = msg.message?.uppercase().orEmpty()
            when {
                msg.flag == "RED" -> {
                    periods += Period(openedAt ?: time, leaderCompletes(lap + 1, time))
                    openedAt = null
                }
                msg.category == "SafetyCar" && "DEPLOYED" in text -> {
                    if (openedAt == null) openedAt = time
                }
                msg.category == "SafetyCar" && "IN THIS LAP" in text -> {
                    periods += Period(openedAt ?: time, leaderCompletes(lap, time))
                    openedAt = null
                }
                msg.category == "SafetyCar" && "ENDING" in text -> {
                    periods += Period(openedAt ?: time, time + VSC_ENDING_COUNTDOWN)
                    openedAt = null
                }
            }
        }
        // Period still open at the end of the data (e.g. race finished behind the safety car)
        openedAt?.let { start -> periods += Period(start, Instant.DISTANT_FUTURE) }
        return periods
    }

    /** A car whose last lap ends this long before the leader finishes did not reach the flag. */
    private val RETIREMENT_MARGIN = 3.seconds * 60

    /**
     * The last lap of a driver who retired (pitting or stopping), or null if they finished.
     * Lapped finishers complete their last lap after the leader takes the flag, retirements well before.
     */
    fun retirementLap(driverLaps: List<Lap>, allLaps: List<Lap>): Int? {
        fun Lap.end() = dateStart?.let { Instant.parse(it) + ((lapDuration ?: 0.0) * 1000).toLong().milliseconds }
        val raceEnd = allLaps.mapNotNull { it.end() }.maxOrNull() ?: return null
        // OpenF1 lists one more lap without a time after a car stops; the last timed lap is the retirement lap
        val last = driverLaps.filter { it.lapDuration != null }.maxByOrNull { it.lapNumber } ?: return null
        val lastEnd = last.end() ?: return null
        return last.lapNumber.takeIf { lastEnd < raceEnd - RETIREMENT_MARGIN }
    }

    /** In-laps: the lap before each pit-out lap, slower because the car enters the pit lane. */
    fun inLaps(driverLaps: List<Lap>): Set<Int> =
        driverLaps.filter { it.isPitOutLap && it.lapNumber > 1 }.map { it.lapNumber - 1 }.toSet()

    /**
     * Laps of [driverNumber] with a valid time that are representative of race pace:
     * not lap 1 (standing start), not a pit in/out lap, not the retirement lap and not overlapping
     * a neutralised period.
     * [laps] should contain every driver's laps so the leader's timing is known.
     */
    fun representativeLaps(laps: List<Lap>, driverNumber: Int, raceControl: List<RaceControlMessage>): List<Lap> {
        val driverLaps = laps.filter { it.driverNumber == driverNumber }
        val periods = neutralisedPeriods(raceControl, laps)
        val inLaps = inLaps(driverLaps)
        val retirementLap = retirementLap(driverLaps, laps)
        return driverLaps.filter { lap ->
            lap.lapNumber > 1 &&
                !lap.isPitOutLap &&
                lap.lapNumber !in inLaps &&
                lap.lapNumber != retirementLap &&
                lap.lapDuration != null && lap.lapDuration > 0 &&
                !lap.overlaps(periods)
        }
    }

    private fun Lap.overlaps(periods: List<Period>): Boolean {
        val start = dateStart?.let(Instant::parse) ?: return false
        val end = start + (lapDuration!! * 1000).toLong().milliseconds
        return periods.any { it.start < end && start < it.end }
    }
}
