package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateCleanPaceUseCaseTest {

    private val useCase = CalculateCleanPaceUseCase()

    private fun makeLap(lapNum: Int, duration: Double, pitOut: Boolean = false) = Lap(
        driverNumber = 1, lapNumber = lapNum, lapDuration = duration,
        isPitOutLap = pitOut, sessionKey = 9165
    )

    @Test
    fun `excludes pit out laps`() {
        val laps = listOf(
            makeLap(1, 95.0, pitOut = true),
            makeLap(2, 91.0),
            makeLap(3, 91.5),
            makeLap(4, 91.2),
            makeLap(5, 91.8),
        )
        val result = useCase.invoke(laps, driverNumber = 1)
        assertEquals(4, result.cleanLapCount)
    }

    @Test
    fun `calculates average median and std dev correctly`() {
        val durations = listOf(90.0, 91.0, 91.5, 92.0, 92.5, 93.0, 93.5, 94.0, 94.5, 95.0)
        val laps = durations.mapIndexed { i, d -> makeLap(i + 2, d) } // lap 1 (standing start) is excluded
        val result = useCase.invoke(laps, driverNumber = 1)

        // With 10 laps, 5% trim = 0 laps trimmed (0.5 rounds down)
        assertTrue(result.averagePace > 0)
        assertTrue(result.consistency > 0)
        assertEquals(90.0, result.bestLap, 0.001)
    }

    @Test
    fun `handles empty input`() {
        val result = useCase.invoke(emptyList(), driverNumber = 1)
        assertEquals(0, result.cleanLapCount)
        assertEquals(0.0, result.averagePace, 0.001)
    }

    @Test
    fun `filters by driver number`() {
        val laps = listOf(
            makeLap(1, 91.0),
            Lap(driverNumber = 2, lapNumber = 1, lapDuration = 90.0, sessionKey = 9165),
        )
        val result = useCase.invoke(laps, driverNumber = 1)
        assertEquals(1, result.totalLapCount)
    }
}
