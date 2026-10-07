package com.fastpilot.ui.screens.strategy

import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.LapRepository
import com.fastpilot.data.repository.PitStopRepository
import com.fastpilot.data.repository.RaceControlRepository
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.StintRepository
import com.fastpilot.domain.usecase.CalculateCleanPaceUseCase
import com.fastpilot.domain.usecase.GetTyreDegradationUseCase
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class StrategyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var raceControlAvailable = true
    private var safetyCar = false

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val sessionStart = Instant.parse("2024-11-03T16:00:00Z")

    // Lap 1 is the standing start; afterwards each lap is `slope` slower on the same tyres.
    // #1 and #44 run 10 laps, #3 retires after 3 laps.
    private data class Car(val base: Double, val slope: Double, val laps: Int, val compound: String)
    private val cars = mapOf(
        1 to Car(base = 90.0, slope = 0.1, laps = 10, compound = "MEDIUM"),
        44 to Car(base = 90.5, slope = 0.05, laps = 10, compound = "HARD"),
        3 to Car(base = 91.0, slope = 0.0, laps = 3, compound = "SOFT")
    )

    /** Laps 5 and 6 run behind the safety car when [safetyCar] is set. */
    private fun lapTime(car: Car, lap: Int) = when {
        lap == 1 -> 100.0
        safetyCar && lap in 5..6 -> 130.0
        else -> car.base + car.slope * (lap - 1)
    }

    private fun lapStart(driver: Int, lap: Int): Instant {
        val car = cars.getValue(driver)
        val elapsed = (1 until lap).sumOf { lapTime(car, it) }
        return sessionStart + (elapsed * 1000).toLong().milliseconds
    }

    private fun lapsJson() = cars.flatMap { (driver, car) ->
        (1..car.laps).map { lap ->
            """{"driver_number":$driver,"lap_number":$lap,"lap_duration":${lapTime(car, lap)},"is_pit_out_lap":false,"date_start":"${lapStart(driver, lap)}"}"""
        }
    }.joinToString(prefix = "[", postfix = "]")

    private fun stintsJson() = cars.entries.joinToString(prefix = "[", postfix = "]") { (driver, car) ->
        """{"driver_number":$driver,"stint_number":1,"lap_start":1,"lap_end":${car.laps},"compound":"${car.compound}","tyre_age_at_start":0}"""
    }

    // Deployed halfway through the leader's lap 5, in this lap on lap 6: laps 5 and 6 are neutralised
    private fun raceControlJson() = if (!safetyCar) "[]" else """[
        {"category":"SafetyCar","message":"SAFETY CAR DEPLOYED","lap_number":5,"date":"${lapStart(1, 5) + 45_000.milliseconds}"},
        {"category":"SafetyCar","message":"SAFETY CAR IN THIS LAP","lap_number":6,"date":"${lapStart(1, 6) + 60_000.milliseconds}"}
    ]"""

    private fun viewModel(): StrategyViewModel {
        val api = OpenF1Api(mockHttpClient(dispatcher) { request ->
            val params = request.url.parameters
            when {
                request.url.encodedPath.endsWith("/sessions") ->
                    respondJson(if (params["year"] == "2024") """[{"session_key":9636,"session_name":"Race","session_type":"Race","year":2024}]""" else "[]")
                request.url.encodedPath.endsWith("/drivers") ->
                    respondJson("""[{"driver_number":1,"name_acronym":"VER"},{"driver_number":44,"name_acronym":"HAM"},{"driver_number":3,"name_acronym":"RIC"}]""")
                request.url.encodedPath.endsWith("/stints") -> respondJson(stintsJson())
                request.url.encodedPath.endsWith("/laps") -> respondJson(lapsJson())
                request.url.encodedPath.endsWith("/race_control") -> {
                    if (!raceControlAvailable) error("race control unavailable")
                    respondJson(raceControlJson())
                }
                else -> respondJson("[]")
            }
        })
        return StrategyViewModel(
            SessionRepository(api, db = null),
            DriverRepository(api, db = null),
            StintRepository(api, db = null),
            PitStopRepository(api),
            LapRepository(api, db = null),
            RaceControlRepository(api),
            CalculateCleanPaceUseCase(),
            GetTyreDegradationUseCase()
        )
    }

    @Test
    fun `ranks drivers by clean pace and computes degradation per stint`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoadingMetrics)
        // #3 retired with too few clean laps for a meaningful pace
        assertEquals(listOf(1, 44), state.cleanPace.map { it.driverNumber })
        assertEquals(90.5, state.cleanPace[0].medianPace, 1e-9)
        assertEquals(90.75, state.cleanPace[1].medianPace, 1e-9)
        assertEquals(0.1, state.degradationByDriver.getValue(1).single().degradationPerLap, 1e-9)
        assertEquals(0.05, state.degradationByDriver.getValue(44).single().degradationPerLap, 1e-9)
        assertEquals("HARD", state.degradationByDriver.getValue(44).single().compound)
        assertFalse(state.metricsWithoutRaceControl)
    }

    @Test
    fun `safety car laps are left out of both metrics`() = runTest(dispatcher) {
        safetyCar = true
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        val leader = state.cleanPace.first { it.driverNumber == 1 }
        assertEquals(7, leader.cleanLapCount)
        assertTrue(leader.cleanLaps.none { it.lapNumber in 5..6 })
        assertEquals(0.1, state.degradationByDriver.getValue(1).single().degradationPerLap, 1e-9)
    }

    @Test
    fun `missing race control still shows the metrics with a warning`() = runTest(dispatcher) {
        raceControlAvailable = false
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(1, 44), state.cleanPace.map { it.driverNumber })
        assertTrue(state.metricsWithoutRaceControl)
        assertEquals(null, state.error)
    }
}
