package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.LapRepository
import com.fastpilot.model.Lap
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetDriverDuelUseCaseTest {

    private fun makeLap(
        driver: Int, lapNum: Int, duration: Double?, pitOut: Boolean = false,
        s1: Double? = null, s2: Double? = null, s3: Double? = null
    ) = Lap(
        driverNumber = driver, lapNumber = lapNum, lapDuration = duration,
        isPitOutLap = pitOut, sector1 = s1, sector2 = s2, sector3 = s3, sessionKey = 9165
    )

    /** OpenF1 JSON for one lap, as returned by /v1/laps. */
    private fun lapJson(driver: Int, lapNum: Int, duration: Double) =
        """{"driver_number":$driver,"lap_number":$lapNum,"lap_duration":$duration,"is_pit_out_lap":false,"session_key":9165}"""

    private val useCase = GetDriverDuelUseCase(
        LapRepository(OpenF1Api(mockHttpClient { respondJson("[]") }), db = null)
    )

    @Test
    fun `cumulative delta accumulates driver2 minus driver1 lap times`() {
        val laps1 = listOf(makeLap(1, 1, 90.0), makeLap(1, 2, 91.0), makeLap(1, 3, 92.0))
        val laps2 = listOf(makeLap(44, 1, 91.0), makeLap(44, 2, 91.5), makeLap(44, 3, 91.0))

        val result = useCase.calculate(1, 44, laps1, laps2)

        assertEquals(listOf(1, 2, 3), result.cumulativeDelta.map { it.lapNumber })
        // +1.0, +0.5, -1.0 -> cumulative 1.0, 1.5, 0.5 (positive = driver1 ahead)
        assertEquals(1.0, result.cumulativeDelta[0].delta, 1e-9)
        assertEquals(1.5, result.cumulativeDelta[1].delta, 1e-9)
        assertEquals(0.5, result.cumulativeDelta[2].delta, 1e-9)
    }

    @Test
    fun `negative delta when driver2 is faster`() {
        val laps1 = listOf(makeLap(1, 1, 92.0), makeLap(1, 2, 92.0))
        val laps2 = listOf(makeLap(44, 1, 91.0), makeLap(44, 2, 91.0))

        val result = useCase.calculate(1, 44, laps1, laps2)

        assertEquals(-2.0, result.cumulativeDelta.last().delta, 1e-9)
    }

    @Test
    fun `ignores pit out laps and laps without duration`() {
        val laps1 = listOf(
            makeLap(1, 1, 95.0, pitOut = true),
            makeLap(1, 2, 91.0),
            makeLap(1, 3, null),
            makeLap(1, 4, 91.0)
        )
        val laps2 = listOf(makeLap(44, 1, 90.0), makeLap(44, 2, 92.0), makeLap(44, 3, 92.0), makeLap(44, 4, 92.0))

        val result = useCase.calculate(1, 44, laps1, laps2)

        assertEquals(listOf(2, 4), result.lapsDriver1.map { it.lapNumber })
        assertEquals(4, result.lapsDriver2.size)
        // Only laps 2 and 4 are common: +1.0 each
        assertEquals(listOf(2, 4), result.cumulativeDelta.map { it.lapNumber })
        assertEquals(2.0, result.cumulativeDelta.last().delta, 1e-9)
    }

    @Test
    fun `only compares laps completed by both drivers`() {
        // Driver 44 retired after lap 2
        val laps1 = (1..5).map { makeLap(1, it, 90.0) }
        val laps2 = (1..2).map { makeLap(44, it, 90.5) }

        val result = useCase.calculate(1, 44, laps1, laps2)

        assertEquals(2, result.cumulativeDelta.size)
        assertEquals(2, result.sectorComparison.size)
    }

    @Test
    fun `no common laps yields empty comparison`() {
        val result = useCase.calculate(1, 44, listOf(makeLap(1, 1, 90.0)), emptyList())

        assertTrue(result.cumulativeDelta.isEmpty())
        assertTrue(result.sectorComparison.isEmpty())
    }

    @Test
    fun `sector comparison pairs both drivers sectors per lap`() {
        val laps1 = listOf(makeLap(1, 1, 90.0, s1 = 30.1, s2 = 30.2, s3 = 29.7))
        val laps2 = listOf(makeLap(44, 1, 90.3, s1 = 30.0, s2 = null, s3 = 30.0))

        val sectors = useCase.calculate(1, 44, laps1, laps2).sectorComparison.single()

        assertEquals(30.1, sectors.s1Driver1)
        assertEquals(30.0, sectors.s1Driver2)
        assertEquals(30.2, sectors.s2Driver1)
        assertNull(sectors.s2Driver2)
        assertEquals(29.7, sectors.s3Driver1)
        assertEquals(30.0, sectors.s3Driver2)
    }

    @Test
    fun `invoke fetches each driver's laps from OpenF1`() = runTest {
        val requestedDrivers = mutableListOf<String?>()
        val api = OpenF1Api(mockHttpClient { request ->
            val driver = request.url.parameters["driver_number"]
            requestedDrivers += driver
            when (driver) {
                "1" -> respondJson("[${lapJson(1, 1, 90.0)},${lapJson(1, 2, 90.0)}]")
                else -> respondJson("[${lapJson(44, 1, 90.4)},${lapJson(44, 2, 90.4)}]")
            }
        })
        val duelUseCase = GetDriverDuelUseCase(LapRepository(api, db = null))

        val result = duelUseCase.invoke(sessionKey = 9165, driver1Number = 1, driver2Number = 44)

        assertEquals(listOf<String?>("1", "44"), requestedDrivers)
        assertEquals(0.8, result.cumulativeDelta.last().delta, 1e-9)
    }
}
