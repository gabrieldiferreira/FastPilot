package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalculateTheoreticalBestUseCaseTest {

    private val useCase = CalculateTheoreticalBestUseCase()

    private val testLaps = listOf(
        Lap(driverNumber = 1, lapNumber = 1, lapDuration = 92.0,
            sector1 = 27.0, sector2 = 39.0, sector3 = 26.0, sessionKey = 9165),
        Lap(driverNumber = 1, lapNumber = 2, lapDuration = 91.5,
            sector1 = 26.5, sector2 = 39.5, sector3 = 25.5, sessionKey = 9165),
        Lap(driverNumber = 1, lapNumber = 3, lapDuration = 91.0,
            sector1 = 27.2, sector2 = 38.5, sector3 = 25.3, sessionKey = 9165),
        Lap(driverNumber = 1, lapNumber = 4, lapDuration = 91.8,
            sector1 = 26.8, sector2 = 38.8, sector3 = 26.2, sessionKey = 9165),
    )

    @Test
    fun `theoretical best takes minimum of each sector`() {
        val result = useCase.invoke(testLaps, driverNumber = 1)
        assertNotNull(result)

        // Best S1 = 26.5 (lap 2), Best S2 = 38.5 (lap 3), Best S3 = 25.3 (lap 3)
        assertEquals(26.5, result.bestSector1, 0.001)
        assertEquals(38.5, result.bestSector2, 0.001)
        assertEquals(25.3, result.bestSector3, 0.001)
        assertEquals(90.3, result.theoreticalBestTime, 0.001)

        // Actual best = 91.0 (lap 3)
        assertEquals(91.0, result.actualBestTime, 0.001)

        // Gap = 91.0 - 90.3 = 0.7
        assertEquals(0.7, result.gap, 0.001)
    }

    @Test
    fun `returns null for empty laps`() {
        val result = useCase.invoke(emptyList(), driverNumber = 1)
        assertEquals(null, result)
    }

    @Test
    fun `filters pit out laps`() {
        val lapsWithPit = testLaps + Lap(
            driverNumber = 1, lapNumber = 5, lapDuration = 120.0,
            sector1 = 24.0, sector2 = 36.0, sector3 = 24.0,
            isPitOutLap = true, sessionKey = 9165
        )
        val result = useCase.invoke(lapsWithPit, driverNumber = 1)
        assertNotNull(result)
        // Should NOT use the pit lap sectors even though they're "faster"
        assertEquals(26.5, result.bestSector1, 0.001)
    }
}
