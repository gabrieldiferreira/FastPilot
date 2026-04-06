package com.fastpilot.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.*
import com.fastpilot.domain.usecase.GetLivePositionsUseCase
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
    private val raceControlRepo: RaceControlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Try 2024 first (guaranteed to have data), then current year
                val sessions2024 = try { sessionRepo.getSessions(year = 2024) } catch (_: Exception) { emptyList() }
                val sessions2025 = try { sessionRepo.getSessions(year = 2025) } catch (_: Exception) { emptyList() }
                val allSessions = (sessions2025 + sessions2024)

                if (allSessions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Nenhuma sessão encontrada. Verifique sua conexão."
                    )
                    return@launch
                }

                // Default to first Race session if available, otherwise first session
                val defaultSession = allSessions.firstOrNull { it.sessionType == "Race" }
                    ?: allSessions.first()

                _uiState.value = _uiState.value.copy(
                    sessions = allSessions,
                    currentSession = defaultSession,
                    isLoading = false
                )
                loadSessionData(defaultSession.sessionKey)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar sessões: ${e.message}"
                )
            }
        }
    }

    fun selectSession(session: Session) {
        _uiState.value = _uiState.value.copy(
            currentSession = session,
            positions = emptyList(),
            intervals = emptyMap(),
            raceControlMessages = emptyList(),
            isLoading = true,
            error = null
        )
        loadSessionData(session.sessionKey)
    }

    private fun loadSessionData(sessionKey: Int) {
        // Cancel previous polling
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            // Load positions (one-shot, no continuous poll for historical data)
            try {
                getPositions.invoke(sessionKey, pollIntervalMs = Long.MAX_VALUE).collect { positions ->
                    _uiState.value = _uiState.value.copy(positions = positions, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar posições: ${e.message}"
                )
            }
        }

        // Intervals
        viewModelScope.launch {
            try {
                val intervals = intervalRepo.getLatestIntervals(sessionKey)
                _uiState.value = _uiState.value.copy(
                    intervals = intervals.associateBy { it.driverNumber }
                )
            } catch (_: Exception) {}
        }

        // Race control
        viewModelScope.launch {
            try {
                val messages = raceControlRepo.getRaceControl(sessionKey)
                _uiState.value = _uiState.value.copy(raceControlMessages = messages)
            } catch (_: Exception) {}
        }
    }
}
