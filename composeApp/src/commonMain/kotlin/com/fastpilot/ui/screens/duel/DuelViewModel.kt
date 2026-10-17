package com.fastpilot.ui.screens.duel

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.defaultSession
import com.fastpilot.data.repository.getRecentSessions
import com.fastpilot.domain.usecase.*
import com.fastpilot.model.Driver
import com.fastpilot.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.fastpilot.ui.ScreenDispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class DuelUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val drivers: List<Driver> = emptyList(),
    val driver1: Driver? = null,
    val driver2: Driver? = null,
    val duelData: DuelData? = null,
    val isComputing: Boolean = false,
    /** Where driver 2 gains/loses time against driver 1 over their fastest laps. */
    val lapDelta: LapDelta? = null,
    val isComputingLapDelta: Boolean = false,
    /** Position data could not be loaded (e.g. OpenF1 rate limit); lap-by-lap comparison still shown. */
    val lapDeltaUnavailable: Boolean = false,
    val error: UiError? = null
)

class DuelViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val duelUseCase: GetDriverDuelUseCase,
    private val lapDeltaUseCase: GetLapDeltaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuelUiState())
    val uiState: StateFlow<DuelUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(
            selectedSession = session, isLoading = true,
            drivers = emptyList(), driver1 = null, driver2 = null, duelData = null, lapDelta = null, lapDeltaUnavailable = false, error = null
        ) }
        viewModelScope.launch(ScreenDispatchers.background) {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                _uiState.update { it.copy(drivers = drivers, isLoading = false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = UiError(ErrorKind.DRIVERS, e.message)) }
            }
        }
    }

    fun selectDriver1(driver: Driver) {
        _uiState.update { it.copy(driver1 = driver, duelData = null, lapDelta = null, lapDeltaUnavailable = false) }
        tryComputeDuel()
    }

    fun selectDriver2(driver: Driver) {
        _uiState.update { it.copy(driver2 = driver, duelData = null, lapDelta = null, lapDeltaUnavailable = false) }
        tryComputeDuel()
    }

    private fun tryComputeDuel() {
        val sessionKey = _uiState.value.selectedSession?.sessionKey ?: return
        val d1 = _uiState.value.driver1 ?: return
        val d2 = _uiState.value.driver2 ?: return
        if (d1.driverNumber == d2.driverNumber) return

        _uiState.update { it.copy(isComputing = true, error = null) }
        viewModelScope.launch(ScreenDispatchers.background) {
            try {
                val duelData = duelUseCase.invoke(sessionKey, d1.driverNumber, d2.driverNumber)
                _uiState.update { it.copy(duelData = duelData, isComputing = false) }
                computeLapDelta(sessionKey, duelData)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isComputing = false,
                    error = UiError(ErrorKind.DUEL, e.message)
                ) }
            }
        }
    }

    private suspend fun computeLapDelta(sessionKey: Int, duel: DuelData) {
        val fastest1 = duel.lapsDriver1.minByOrNull { it.lapDuration!! } ?: return
        val fastest2 = duel.lapsDriver2.minByOrNull { it.lapDuration!! } ?: return

        _uiState.update { it.copy(isComputingLapDelta = true, lapDeltaUnavailable = false) }
        try {
            val lapDelta = lapDeltaUseCase(sessionKey, fastest1, fastest2)
            _uiState.update { it.copy(
                lapDelta = lapDelta, isComputingLapDelta = false, lapDeltaUnavailable = lapDelta == null
            ) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // The lap-by-lap comparison is still useful without position data
            _uiState.update { it.copy(isComputingLapDelta = false, lapDeltaUnavailable = true) }
        }
    }
}
