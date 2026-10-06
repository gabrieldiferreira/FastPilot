package com.fastpilot.ui.screens.standings

import com.fastpilot.data.repository.StandingsRepository
import com.fastpilot.domain.usecase.CalculateChampionshipGapsUseCase
import com.fastpilot.network.JolpicaApi
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StandingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val requestedPaths = mutableListOf<String>()
    private var failRequests = false

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun driverStandings(season: String, leaderPoints: Int) = """
        {"MRData":{"StandingsTable":{"StandingsLists":[{"season":"$season","round":"22",
        "DriverStandings":[
          {"position":"1","points":"$leaderPoints","wins":"9","Driver":{"driverId":"a","code":"AAA","givenName":"A","familyName":"Driver"},"Constructors":[{"constructorId":"mclaren","name":"McLaren"}]},
          {"position":"2","points":"300","wins":"3","Driver":{"driverId":"b","code":"BBB","givenName":"B","familyName":"Driver"},"Constructors":[{"constructorId":"ferrari","name":"Ferrari"}]}
        ]}]}}}
    """.trimIndent()

    private val constructorStandings = """
        {"MRData":{"StandingsTable":{"StandingsLists":[{"round":"22",
        "ConstructorStandings":[{"position":"1","points":"600","wins":"12","Constructor":{"constructorId":"mclaren","name":"McLaren"}}]
        }]}}}
    """.trimIndent()

    private fun viewModel(): StandingsViewModel {
        val api = JolpicaApi(mockHttpClient(dispatcher) { request ->
            val path = request.url.encodedPath
            requestedPaths += path
            if (failRequests) error("offline")
            when {
                "constructorStandings" in path -> respondJson(constructorStandings)
                "/2024/" in path -> respondJson(driverStandings("2024", 437))
                else -> respondJson(driverStandings("2025", 423))
            }
        })
        return StandingsViewModel(StandingsRepository(api, db = null), CalculateChampionshipGapsUseCase())
    }

    @Test
    fun `loads the latest season on start`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2025, state.selectedSeason)
        assertNull(state.error)
        val tables = assertNotNull(state.tables)
        assertEquals(22, tables.round)
        assertEquals(423.0, tables.drivers.leaderPoints)
        assertEquals(123.0, tables.drivers.rows[1].gapToLeader)
        assertEquals(1, tables.constructors.rows.size)
    }

    @Test
    fun `selecting another season reloads standings`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectSeason(2024)
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.tables)
        advanceUntilIdle()

        assertEquals(2024, vm.uiState.value.selectedSeason)
        assertEquals(437.0, vm.uiState.value.tables?.drivers?.leaderPoints)
        assertTrue(requestedPaths.any { "/2024/driverStandings" in it })
    }

    @Test
    fun `mode switch does not refetch`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val requestsBefore = requestedPaths.size

        vm.selectMode(StandingsMode.CONSTRUCTORS)
        advanceUntilIdle()

        assertEquals(StandingsMode.CONSTRUCTORS, vm.uiState.value.mode)
        assertEquals(requestsBefore, requestedPaths.size)
        assertNotNull(vm.uiState.value.tables)
    }

    @Test
    fun `network failure surfaces an error`() = runTest(dispatcher) {
        failRequests = true
        val vm = viewModel()

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.tables)
        assertTrue(state.error!!.startsWith("Erro ao carregar classificação"))
    }

    @Test
    fun `refresh recovers after a failure`() = runTest(dispatcher) {
        failRequests = true
        val vm = viewModel()
        advanceUntilIdle()

        failRequests = false
        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.tables)
    }
}
