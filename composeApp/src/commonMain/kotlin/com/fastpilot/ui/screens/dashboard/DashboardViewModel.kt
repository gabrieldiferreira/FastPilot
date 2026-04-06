package com.fastpilot.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.polling.LivePollingManager
import com.fastpilot.data.repository.*
import com.fastpilot.domain.usecase.GetLivePositionsUseCase
import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.domain.usecase.PositionWithDriver
import com.fastpilot.model.Interval
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Session
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isLive: Boolean = false,
    val currentSession: Session? = null,
    val sessions: List<Session> = emptyList(),
    val positions: List<PositionWithDriver> = emptyList(),
    val intervals: Map<Int, Interval> = emptyMap(),
    val raceControlMessages: List<RaceControlMessage> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(
    private val sessionRepo: SessionRepository,
    private val getPositions: GetLivePositionsUseCase,
    private val intervalRepo: IntervalRepository,
    private val raceControlRepo: RaceControlRepository,
    private val livePolling: LivePollingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null
    private var intervalPollingJob: Job? = null
    private var raceControlPollingJob: Job? = null

    companion object {
        private val YEARS = listOf(2025, 2024)
    }

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val allSessions = YEARS.flatMap { year ->
                    try { sessionRepo.getSessions(year = year) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { emptyList() }
                }

                if (allSessions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Nenhuma sessão encontrada. Verifique sua conexão."
                    )
                    return@launch
                }

                // Check for a live session first
                val liveSession = LiveSessionDetector.findLiveSession(allSessions)

                // Default: live session > first Race > first session
                val defaultSession = liveSession
                    ?: allSessions.firstOrNull { it.sessionType == "Race" }
                    ?: allSessions.first()

                _uiState.value = _uiState.value.copy(
                    sessions = allSessions,
                    currentSession = defaultSession,
                    isLive = liveSession != null && liveSession.sessionKey == defaultSession.sessionKey,
                    isLoading = false
                )
                loadSessionData(defaultSession)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar sessões: ${e.message}"
                )
            }
        }
    }

    fun selectSession(session: Session) {
        val isLive = LiveSessionDetector.isLive(session)
        _uiState.value = _uiState.value.copy(
            currentSession = session,
            isLive = isLive,
            positions = emptyList(),
            intervals = emptyMap(),
            raceControlMessages = emptyList(),
            isLoading = true,
            error = null
        )
        loadSessionData(session)
    }

    private fun loadSessionData(session: Session) {
        // Cancel previous polling jobs
        pollingJob?.cancel()
        intervalPollingJob?.cancel()
        raceControlPollingJob?.cancel()

        val sessionKey = session.sessionKey
        val isLive = LiveSessionDetector.isLive(session)

        if (isLive) {
            // ── LIVE MODE: continuous polling ───────────────────────
            pollingJob = viewModelScope.launch {
                getPositions.invoke(sessionKey, pollIntervalMs = LivePollingManager.POSITION_POLL_MS).collect { positions ->
                    _uiState.value = _uiState.value.copy(positions = positions, isLoading = false)
                }
            }

            intervalPollingJob = viewModelScope.launch {
                livePolling.observeIntervals(sessionKey).collect { intervals ->
                    _uiState.value = _uiState.value.copy(
                        intervals = intervals.associateBy { it.driverNumber }
                    )
                }
            }

            raceControlPollingJob = viewModelScope.launch {
                livePolling.observeRaceControl(sessionKey).collect { messages ->
                    _uiState.value = _uiState.value.copy(raceControlMessages = messages)
                }
            }
        } else {
            // ── HISTORICAL MODE: one-shot fetch ────────────────────
            pollingJob = viewModelScope.launch {
                try {
                    getPositions.invoke(sessionKey, pollIntervalMs = Long.MAX_VALUE).collect { positions ->
                        _uiState.value = _uiState.value.copy(positions = positions, isLoading = false)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar posições: ${e.message}"
                    )
                }
            }

            // Intervals (one-shot)
            viewModelScope.launch {
                try {
                    val intervals = intervalRepo.getLatestIntervals(sessionKey)
                    _uiState.value = _uiState.value.copy(
                        intervals = intervals.associateBy { it.driverNumber }
                    )
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
            }

            // Race control (one-shot)
            viewModelScope.launch {
                try {
                    val messages = raceControlRepo.getRaceControl(sessionKey)
                    _uiState.value = _uiState.value.copy(raceControlMessages = messages)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
            }
        }
    }
}
