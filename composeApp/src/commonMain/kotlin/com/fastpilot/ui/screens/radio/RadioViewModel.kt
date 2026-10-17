package com.fastpilot.ui.screens.radio

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.polling.LivePollingManager
import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.TeamRadioRepository
import com.fastpilot.data.repository.defaultSession
import com.fastpilot.data.repository.getRecentSessions
import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.model.Driver
import com.fastpilot.model.Session
import com.fastpilot.model.TeamRadio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.fastpilot.ui.ScreenDispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class RadioUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val drivers: List<Driver> = emptyList(),
    val selectedDriver: Driver? = null,
    val radios: List<TeamRadio> = emptyList(),
    val error: UiError? = null
)

class RadioViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val radioRepo: TeamRadioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()
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
            radios = emptyList(), selectedDriver = null, error = null
        ) }
        liveJob = viewModelScope.launch(ScreenDispatchers.background) {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                val radios = radioRepo.getTeamRadio(session.sessionKey)
                _uiState.update { it.copy(
                    drivers = drivers, radios = radios, isLoading = false
                ) }
                // Live session: new messages are fetched every 15 s while it lasts
                while (LiveSessionDetector.isLive(session)) {
                    delay(LivePollingManager.TEAM_RADIO_POLL_MS)
                    try {
                        // Fetched before reading the state, which may change during the request
                        val radios = radioRepo.getTeamRadio(session.sessionKey)
                        _uiState.update { it.copy(radios = radios) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) { /* retry on next poll */ }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false, error = UiError(ErrorKind.RADIO, e.message)
                ) }
            }
        }
    }

    fun filterByDriver(driver: Driver?) {
        _uiState.update { it.copy(selectedDriver = driver) }
    }
}
