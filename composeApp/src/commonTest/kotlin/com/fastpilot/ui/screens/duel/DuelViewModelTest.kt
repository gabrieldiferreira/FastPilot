package com.fastpilot.ui.screens.duel

import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.LapRepository
import com.fastpilot.data.repository.LocationRepository
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.availableSeasons
import com.fastpilot.domain.usecase.CalculateLapDeltaUseCase
import com.fastpilot.domain.usecase.GetDriverDuelUseCase
import com.fastpilot.domain.usecase.GetLapDeltaUseCase
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class DuelViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var locationAvailable = true

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Lap 2 is each driver's fastest: 90.0 s for #1, 90.5 s for #44
    private val lapTimes = mapOf(1 to listOf(91.0, 90.0, 91.2), 44 to listOf(91.5, 90.5, 91.0))
    private val sessionStart = Instant.parse("2024-11-03T16:00:00Z")

    private fun lapStart(driver: Int, lap: Int) =
        sessionStart + (lapTimes.getValue(driver).take(lap - 1).sum() * 1000).toLong().milliseconds

    private fun lapsJson(driver: Int) = lapTimes.getValue(driver).mapIndexed { i, duration ->
        """{"driver_number":$driver,"lap_number":${i + 1},"lap_duration":$duration,"is_pit_out_lap":false,"date_start":"${lapStart(driver, i + 1)}"}"""
    }.joinToString(prefix = "[", postfix = "]")

    /** A car driving a 2 km loop at constant speed during the requested window. */
    private fun locationJson(driver: Int, from: Instant): String {
        val duration = lapTimes.getValue(driver)[1]
        return (0 until (duration / 0.25).toInt()).joinToString(prefix = "[", postfix = "]") { i ->
            val t = i * 0.25 + 0.1
            val angle = 2 * PI * t / duration
            val x = (4000 * cos(angle)).roundToInt()
            val y = (2366 * sin(angle)).roundToInt()
            """{"driver_number":$driver,"x":$x,"y":$y,"z":0,"date":"${from + (t * 1000).toLong().milliseconds}"}"""
        }
    }

    private fun viewModel(): DuelViewModel {
        val api = OpenF1Api(mockHttpClient(dispatcher) { request ->
            val params = request.url.parameters
            when {
                request.url.encodedPath.endsWith("/sessions") ->
                    respondJson(if (params["year"] == availableSeasons().last().toString()) """[{"session_key":9636,"session_name":"Race","session_type":"Race","year":2024}]""" else "[]")
                request.url.encodedPath.endsWith("/drivers") ->
                    respondJson("""[{"driver_number":1,"name_acronym":"VER"},{"driver_number":44,"name_acronym":"HAM"}]""")
                request.url.encodedPath.endsWith("/laps") -> respondJson(lapsJson(params["driver_number"]!!.toInt()))
                request.url.encodedPath.endsWith("/location") -> {
                    if (!locationAvailable) error("location unavailable")
                    val driver = params["driver_number"]!!.toInt()
                    respondJson(locationJson(driver, Instant.parse(params["date>"]!!)))
                }
                else -> respondJson("[]")
            }
        })
        return DuelViewModel(
            SessionRepository(api, db = null),
            DriverRepository(api, db = null),
            GetDriverDuelUseCase(LapRepository(api, db = null)),
            GetLapDeltaUseCase(LocationRepository(api), CalculateLapDeltaUseCase())
        )
    }

    private suspend fun kotlinx.coroutines.test.TestScope.selectBothDrivers(vm: DuelViewModel) {
        advanceUntilIdle()
        val drivers = vm.uiState.value.drivers
        vm.selectDriver1(drivers.first { it.driverNumber == 1 })
        vm.selectDriver2(drivers.first { it.driverNumber == 44 })
        advanceUntilIdle()
    }

    @Test
    fun `computes the lap delta between both drivers fastest laps`() = runTest(dispatcher) {
        val vm = viewModel()

        selectBothDrivers(vm)

        val state = vm.uiState.value
        assertNotNull(state.duelData)
        val lapDelta = assertNotNull(state.lapDelta)
        assertEquals(2, lapDelta.referenceLapNumber)
        assertEquals(2, lapDelta.comparedLapNumber)
        assertEquals(0.5, lapDelta.finalGap, 1e-9)
        assertFalse(state.isComputingLapDelta)
        assertFalse(state.lapDeltaUnavailable)
    }

    @Test
    fun `missing position data keeps the lap by lap comparison`() = runTest(dispatcher) {
        locationAvailable = false
        val vm = viewModel()

        selectBothDrivers(vm)

        val state = vm.uiState.value
        assertNotNull(state.duelData)
        assertNull(state.lapDelta)
        assertTrue(state.lapDeltaUnavailable)
        assertNull(state.error)
        assertFalse(state.isComputingLapDelta)
    }

    @Test
    fun `changing a driver clears the previous lap delta`() = runTest(dispatcher) {
        val vm = viewModel()
        selectBothDrivers(vm)

        vm.selectDriver2(vm.uiState.value.drivers.first { it.driverNumber == 44 })

        assertNull(vm.uiState.value.lapDelta)
    }
}
