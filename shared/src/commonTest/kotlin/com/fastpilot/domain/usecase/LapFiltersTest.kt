package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Stint
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class LapFiltersTest {

    private val raceStart = Instant.parse("2023-09-17T12:00:00Z")
    private val lapTime = 100.0

    /** Leader (#1) and a car one lap down (#2, starts each lap one lap later), 100 s laps. */
    private fun field(laps: IntRange, lapTimes: (driver: Int, lap: Int) -> Double = { _, _ -> lapTime }): List<Lap> =
        listOf(1 to 0, 2 to 1).flatMap { (driver, lapsDown) ->
            laps.map { n ->
                Lap(
                    driverNumber = driver, lapNumber = n, lapDuration = lapTimes(driver, n),
                    dateStart = (raceStart + ((n - 1 + lapsDown) * lapTime).seconds).toString()
                )
            }
        }

    /** Message sent [into] seconds after the leader started [lap]. */
    private fun msg(lap: Int, category: String, message: String, flag: String? = null, into: Double = 30.0) =
        RaceControlMessage(
            category = category, flag = flag, message = message, lapNumber = lap,
            date = (raceStart + ((lap - 1) * lapTime + into).seconds).toString()
        )

    private fun representative(laps: List<Lap>, driver: Int, messages: List<RaceControlMessage>) =
        LapFilters.representativeLaps(laps, driver, messages).map { it.lapNumber }

    @Test
    fun `safety car period covers every lap from deployment to in this lap`() {
        // Singapore 2023: deployed on lap 20, in on lap 22
        val laps = field(1..30)
        val messages = listOf(
            msg(20, "SafetyCar", "SAFETY CAR DEPLOYED"),
            msg(22, "SafetyCar", "SAFETY CAR IN THIS LAP")
        )

        assertEquals((2..19) + (23..30), representative(laps, 1, messages))
    }

    @Test
    fun `a lapped car is neutralised on its own lap numbers`() {
        // Race control says lap 20-22 (leader's laps); the car one lap down is on laps 19-21 then
        val laps = field(1..30)
        val messages = listOf(
            msg(20, "SafetyCar", "SAFETY CAR DEPLOYED"),
            msg(22, "SafetyCar", "SAFETY CAR IN THIS LAP")
        )

        assertEquals((2..18) + (22..30), representative(laps, 2, messages))
    }

    @Test
    fun `virtual safety car ends shortly after the ending message`() {
        val laps = field(1..10)
        val messages = listOf(
            msg(4, "SafetyCar", "VIRTUAL SAFETY CAR DEPLOYED", into = 50.0),
            msg(5, "SafetyCar", "VIRTUAL SAFETY CAR ENDING", into = 60.0)
        )

        assertEquals(listOf(2, 3, 6, 7, 8, 9, 10), representative(laps, 1, messages))
    }

    @Test
    fun `red flag closes the safety car period and neutralises the restart lap`() {
        // São Paulo 2024: SC on lap 30, red flag on lap 32
        val laps = field(28..36)
        val messages = listOf(
            msg(30, "SafetyCar", "SAFETY CAR DEPLOYED"),
            msg(32, "Flag", "RED FLAG", flag = "RED")
        )

        assertEquals(listOf(28, 29, 34, 35, 36), representative(laps, 1, messages))
    }

    @Test
    fun `local yellow flags do not neutralise the lap`() {
        val laps = field(1..5)
        val messages = listOf(
            msg(3, "Flag", "YELLOW IN TRACK SECTOR 7", flag = "YELLOW"),
            msg(3, "Flag", "DOUBLE YELLOW IN TRACK SECTOR 7", flag = "DOUBLE YELLOW")
        )

        assertEquals(listOf(2, 3, 4, 5), representative(laps, 1, messages))
    }

    @Test
    fun `period still open at the end of the data runs to the end`() {
        val laps = field(50..58)
        val messages = listOf(msg(55, "SafetyCar", "SAFETY CAR DEPLOYED"))

        assertEquals(listOf(50, 51, 52, 53, 54), representative(laps, 1, messages))
    }

    @Test
    fun `excludes the standing start lap and the in lap before each pit out lap`() {
        val laps = listOf(
            Lap(driverNumber = 1, lapNumber = 1, lapDuration = 104.0),
            Lap(driverNumber = 1, lapNumber = 2, lapDuration = 92.0),
            Lap(driverNumber = 1, lapNumber = 3, lapDuration = 98.5),
            Lap(driverNumber = 1, lapNumber = 4, lapDuration = 112.0, isPitOutLap = true),
            Lap(driverNumber = 1, lapNumber = 5, lapDuration = 91.5)
        )

        assertEquals(setOf(3), LapFilters.inLaps(laps))
        assertEquals(listOf(2, 5), representative(laps, 1, emptyList()))
    }

    @Test
    fun `last lap of a retired car is excluded but a lapped finisher keeps it`() {
        // #1 leads 30 laps; #2 (one lap down) finishes after the leader; #3 retires into the pits on lap 13
        // OpenF1 also lists an untimed lap 14 once the car is in the garage
        val retired = (1..14).map { n ->
            Lap(
                driverNumber = 3, lapNumber = n, lapDuration = when (n) { 13 -> 122.0; 14 -> null; else -> lapTime },
                dateStart = (raceStart + ((n - 1) * lapTime).seconds).toString()
            )
        }
        val laps = field(1..30) + retired

        assertEquals(13, LapFilters.retirementLap(retired, laps))
        assertEquals(null, LapFilters.retirementLap(laps.filter { it.driverNumber == 2 }, laps))
        assertEquals((2..12).toList(), representative(laps, 3, emptyList()))
        assertEquals((2..30).toList(), representative(laps, 2, emptyList()))
    }

    @Test
    fun `degradation ignores the in lap and safety car laps`() {
        val stint = Stint(driverNumber = 1, stintNumber = 1, compound = "MEDIUM", lapStart = 2, lapEnd = 20, tyreAgeAtStart = 0)
        // 0.05 s/lap degradation, SC on laps 8-10 (+20 s) and in-lap 20 (+6 s), pit-out on 21
        val laps = field(1..21) { driver, n ->
            val base = 91.0 + (n - 2) * 0.05
            if (driver != 1) lapTime else when (n) { in 8..10 -> base + 20; 20 -> base + 6; else -> base }
        }.map { if (it.driverNumber == 1 && it.lapNumber == 21) it.copy(isPitOutLap = true) else it }
        val messages = listOf(msg(8, "SafetyCar", "SAFETY CAR DEPLOYED"), msg(10, "SafetyCar", "SAFETY CAR IN THIS LAP"))

        val curve = GetTyreDegradationUseCase().invoke(laps, listOf(stint), 1, messages).single()

        assertEquals(0.05, curve.degradationPerLap, 1e-9)
        assertEquals(15, curve.dataPoints.size) // laps 2-20 minus SC laps 8-10 and in-lap 20
    }
}
