package com.fastpilot.ui.screens.telemetry

import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.LapRepository
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.TelemetryRepository
import com.fastpilot.domain.usecase.CalculateTheoreticalBestUseCase
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Best S1 on lap 2, best S2 on lap 3, best S3 on lap 4; the pit-out lap 1 has the fastest
    // S3 on paper but is not a flying lap
    private val lapsJson = """[
        {"driver_number":1,"lap_number":1,"lap_duration":95.0,"duration_sector_1":32.0,"duration_sector_2":40.0,"duration_sector_3":23.0,"is_pit_out_lap":true},
        {"driver_number":1,"lap_number":2,"lap_duration":90.6,"duration_sector_1":29.9,"duration_sector_2":36.4,"duration_sector_3":24.3,"is_pit_out_lap":false},
        {"driver_number":1,"lap_number":3,"lap_duration":90.5,"duration_sector_1":30.1,"duration_sector_2":36.1,"duration_sector_3":24.3,"is_pit_out_lap":false},
        {"driver_number":1,"lap_number":4,"lap_duration":90.7,"duration_sector_1":30.2,"duration_sector_2":36.5,"duration_sector_3":24.0,"is_pit_out_lap":false}
    ]"""

    private fun viewModel(): TelemetryViewModel {
        val api = OpenF1Api(mockHttpClient(dispatcher) { request ->
            val params = request.url.parameters
            when {
                request.url.encodedPath.endsWith("/sessions") ->
                    respondJson(if (params["year"] == "2024") """[{"session_key":9636,"session_name":"Race","session_type":"Race","year":2024}]""" else "[]")
                request.url.encodedPath.endsWith("/drivers") ->
                    respondJson("""[{"driver_number":1,"name_acronym":"VER"},{"driver_number":44,"name_acronym":"HAM"}]""")
                request.url.encodedPath.endsWith("/laps") ->
                    respondJson(if (params["driver_number"] == "1") lapsJson else "[]")
                else -> respondJson("[]")
            }
        })
        return TelemetryViewModel(
            SessionRepository(api, db = null),
            DriverRepository(api, db = null),
            LapRepository(api, db = null),
            TelemetryRepository(api),
            CalculateTheoreticalBestUseCase()
        )
    }

    @Test
    fun `selecting a driver computes the theoretical best lap`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectDriver(vm.uiState.value.drivers.first { it.driverNumber == 1 })
        advanceUntilIdle()

        val best = assertNotNull(vm.uiState.value.theoreticalBest)
        assertEquals(2, best.bestSector1Lap)
        assertEquals(3, best.bestSector2Lap)
        assertEquals(4, best.bestSector3Lap)
        assertEquals(29.9 + 36.1 + 24.0, best.theoreticalBestTime, 1e-9)
        assertEquals(90.5, best.actualBestTime, 1e-9)
        assertEquals(90.5 - 90.0, best.gap, 1e-9)
    }

    @Test
    fun `changing driver clears the previous theoretical best`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.selectDriver(vm.uiState.value.drivers.first { it.driverNumber == 1 })
        advanceUntilIdle()

        vm.selectDriver(vm.uiState.value.drivers.first { it.driverNumber == 44 })
        advanceUntilIdle()

        assertNull(vm.uiState.value.theoreticalBest)
    }
}
