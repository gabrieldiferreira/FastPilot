package com.fastpilot.ui.screens.weather

import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.WeatherRepository
import com.fastpilot.data.repository.availableSeasons
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import com.fastpilot.ui.ScreenDispatchers
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ScreenDispatchers.background = dispatcher
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        ScreenDispatchers.background = Dispatchers.Default
    }

    private val currentSeason = availableSeasons().first()

    private fun session(key: Int, name: String, year: Int) =
        """{"session_key":$key,"session_name":"$name","session_type":"$name","circuit_short_name":"Interlagos","country_name":"Brazil","year":$year}"""

    private fun viewModel(weatherJson: (sessionKey: String?) -> String): WeatherViewModel {
        val api = OpenF1Api(mockHttpClient(dispatcher) { request ->
            when {
                request.url.encodedPath.endsWith("/sessions") -> when (request.url.parameters["year"]) {
                    // Practice is listed first: the ViewModel must still pick the race
                    "$currentSeason" -> respondJson("[${session(1, "Practice", currentSeason)},${session(2, "Race", currentSeason)}]")
                    "${currentSeason - 1}" -> respondJson("[${session(3, "Race", currentSeason - 1)}]")
                    else -> respondJson("[]")
                }
                else -> respondJson(weatherJson(request.url.parameters["session_key"]))
            }
        })
        return WeatherViewModel(SessionRepository(api, db = null), WeatherRepository(api))
    }

    @Test
    fun `auto selects the latest race and shows its last weather sample`() = runTest(dispatcher) {
        val vm = viewModel { key ->
            if (key == "2") """[{"air_temperature":20.0,"rainfall":0},{"air_temperature":21.5,"rainfall":1}]"""
            else "[]"
        }

        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.sessions.size)
        assertEquals(2, state.selectedSession?.sessionKey)
        assertEquals(2, state.weatherHistory.size)
        assertEquals(21.5, state.currentWeather?.airTemperature)
        assertEquals(1, state.currentWeather?.rainfall)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `selecting a session without weather data clears the current weather`() = runTest(dispatcher) {
        val vm = viewModel { key -> if (key == "2") """[{"air_temperature":20.0}]""" else "[]" }
        advanceUntilIdle()

        vm.selectSession(vm.uiState.value.sessions.first { it.sessionKey == 1 })
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.selectedSession?.sessionKey)
        assertNull(state.currentWeather)
        assertTrue(state.weatherHistory.isEmpty())
    }
}
