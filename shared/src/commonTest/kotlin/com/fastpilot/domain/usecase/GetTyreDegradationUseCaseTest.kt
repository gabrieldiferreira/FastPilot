package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.Stint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetTyreDegradationUseCaseTest {

    private val useCase = GetTyreDegradationUseCase()

    @Test
    fun `calculates degradation curve for a stint`() {
        val stints = listOf(
            Stint(driverNumber = 1, stintNumber = 1, compound = "MEDIUM",
                lapStart = 2, lapEnd = 11, tyreAgeAtStart = 0, sessionKey = 9165)
        )

        // Simulate degradation: each lap 0.1s slower (from lap 2: lap 1 is the standing start)
        val laps = (2..11).map { lapNum ->
            Lap(driverNumber = 1, lapNumber = lapNum,
                lapDuration = 91.0 + (lapNum - 2) * 0.1, sessionKey = 9165)
        }

        val result = useCase.invoke(laps, stints, driverNumber = 1)
        assertEquals(1, result.size)

        val curve = result.first()
        assertEquals("MEDIUM", curve.compound)
        assertEquals(10, curve.dataPoints.size)

        // Degradation should be approximately 0.1s per lap
        assertTrue(curve.degradationPerLap > 0.05)
        assertTrue(curve.degradationPerLap < 0.15)
    }

    @Test
    fun `skips stints with fewer than 3 laps`() {
        val stints = listOf(
            Stint(driverNumber = 1, stintNumber = 1, compound = "SOFT",
                lapStart = 1, lapEnd = 2, tyreAgeAtStart = 0, sessionKey = 9165)
        )
        val laps = listOf(
            Lap(driverNumber = 1, lapNumber = 1, lapDuration = 91.0, sessionKey = 9165),
            Lap(driverNumber = 1, lapNumber = 2, lapDuration = 91.2, sessionKey = 9165)
        )

        val result = useCase.invoke(laps, stints, driverNumber = 1)
        assertEquals(0, result.size)
    }

    @Test
    fun `filters by driver number`() {
        val stints = listOf(
            Stint(driverNumber = 1, stintNumber = 1, compound = "HARD",
                lapStart = 1, lapEnd = 5, tyreAgeAtStart = 0, sessionKey = 9165),
            Stint(driverNumber = 2, stintNumber = 1, compound = "SOFT",
                lapStart = 1, lapEnd = 5, tyreAgeAtStart = 0, sessionKey = 9165)
        )
        val laps = (1..5).flatMap { lapNum ->
            listOf(
                Lap(driverNumber = 1, lapNumber = lapNum, lapDuration = 91.0 + lapNum * 0.05, sessionKey = 9165),
                Lap(driverNumber = 2, lapNumber = lapNum, lapDuration = 90.0 + lapNum * 0.1, sessionKey = 9165)
            )
        }

        val result = useCase.invoke(laps, stints, driverNumber = 1)
        assertEquals(1, result.size)
        assertEquals("HARD", result.first().compound)
    }
}
