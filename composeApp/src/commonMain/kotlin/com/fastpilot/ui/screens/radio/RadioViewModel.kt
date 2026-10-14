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

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val all = sessionRepo.getRecentSessions()
                _uiState.value = _uiState.value.copy(sessions = all, isLoading = false)
                val auto = all.defaultSession()
                auto?.let { selectSession(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiError(ErrorKind.SESSIONS, e.message))
            }
        }
    }

    fun selectSession(session: Session) {
        liveJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedSession = session, isLoading = true,
            radios = emptyList(), selectedDriver = null, error = null
        )
        liveJob = viewModelScope.launch {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                val radios = radioRepo.getTeamRadio(session.sessionKey)
                _uiState.value = _uiState.value.copy(
                    drivers = drivers, radios = radios, isLoading = false
                )
                // Live session: new messages are fetched every 15 s while it lasts
                while (LiveSessionDetector.isLive(session)) {
                    delay(LivePollingManager.TEAM_RADIO_POLL_MS)
                    try {
                        // Fetched before reading the state, which may change during the request
                        val radios = radioRepo.getTeamRadio(session.sessionKey)
                        _uiState.value = _uiState.value.copy(radios = radios)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) { /* retry on next poll */ }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = UiError(ErrorKind.RADIO, e.message)
                )
            }
        }
    }

    fun filterByDriver(driver: Driver?) {
        _uiState.value = _uiState.value.copy(selectedDriver = driver)
    }
}
