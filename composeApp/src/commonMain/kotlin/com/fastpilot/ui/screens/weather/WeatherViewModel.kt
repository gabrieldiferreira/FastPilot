package com.fastpilot.ui.screens.weather

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.polling.LivePollingManager
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.WeatherRepository
import com.fastpilot.data.repository.defaultSession
import com.fastpilot.data.repository.getRecentSessions
import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.model.Session
import com.fastpilot.model.Weather
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.fastpilot.ui.ScreenDispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class WeatherUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val currentWeather: Weather? = null,
    val weatherHistory: List<Weather> = emptyList(),
    val error: UiError? = null
)

class WeatherViewModel(
    private val sessionRepo: SessionRepository,
    private val weatherRepo: WeatherRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    private var liveJob: Job? = null

    init { loadSessions() }

    // Picking the session (and the jobs it starts) stays on the main thread; the loading runs in the background
    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val all = withContext(ScreenDispatchers.background) { sessionRepo.getRecentSessions() }
                _uiState.update { it.copy(sessions = all, isLoading = false) }
                val auto = all.defaultSession()
                auto?.let { selectSession(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = UiError(ErrorKind.SESSIONS, e.message)) }
            }
        }
    }

    fun selectSession(session: Session) {
        liveJob?.cancel()
        _uiState.update { it.copy(
            selectedSession = session, isLoading = true,
            currentWeather = null, weatherHistory = emptyList(), error = null
        ) }
        liveJob = viewModelScope.launch(ScreenDispatchers.background) {
            try {
                val history = weatherRepo.getWeather(session.sessionKey)
                _uiState.update { it.copy(
                    weatherHistory = history,
                    currentWeather = history.lastOrNull(),
                    isLoading = false
                ) }
                // Live session: OpenF1 publishes a weather sample per minute
                while (LiveSessionDetector.isLive(session)) {
                    delay(LivePollingManager.WEATHER_POLL_MS)
                    try {
                        val latest = weatherRepo.getWeather(session.sessionKey)
                        _uiState.update { it.copy(weatherHistory = latest, currentWeather = latest.lastOrNull()) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) { /* retry on next poll */ }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = UiError(ErrorKind.WEATHER, e.message)
                ) }
            }
        }
    }
}
