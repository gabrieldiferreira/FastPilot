package com.fastpilot.ui.screens.duel

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
    val error: String? = null
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
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectSession(session: Session) {
        _uiState.value = _uiState.value.copy(
            selectedSession = session, isLoading = true,
            drivers = emptyList(), driver1 = null, driver2 = null, duelData = null, lapDelta = null, lapDeltaUnavailable = false, error = null
        )
        viewModelScope.launch {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                _uiState.value = _uiState.value.copy(drivers = drivers, isLoading = false)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectDriver1(driver: Driver) {
        _uiState.value = _uiState.value.copy(driver1 = driver, duelData = null, lapDelta = null, lapDeltaUnavailable = false)
        tryComputeDuel()
    }

    fun selectDriver2(driver: Driver) {
        _uiState.value = _uiState.value.copy(driver2 = driver, duelData = null, lapDelta = null, lapDeltaUnavailable = false)
        tryComputeDuel()
    }

    private fun tryComputeDuel() {
        val sessionKey = _uiState.value.selectedSession?.sessionKey ?: return
        val d1 = _uiState.value.driver1 ?: return
        val d2 = _uiState.value.driver2 ?: return
        if (d1.driverNumber == d2.driverNumber) return

        _uiState.value = _uiState.value.copy(isComputing = true, error = null)
        viewModelScope.launch {
            try {
                val duelData = duelUseCase.invoke(sessionKey, d1.driverNumber, d2.driverNumber)
                _uiState.value = _uiState.value.copy(duelData = duelData, isComputing = false)
                computeLapDelta(sessionKey, duelData)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isComputing = false,
                    error = "Erro ao comparar pilotos: ${e.message}"
                )
            }
        }
    }

    private suspend fun computeLapDelta(sessionKey: Int, duel: DuelData) {
        val fastest1 = duel.lapsDriver1.minByOrNull { it.lapDuration!! } ?: return
        val fastest2 = duel.lapsDriver2.minByOrNull { it.lapDuration!! } ?: return

        _uiState.value = _uiState.value.copy(isComputingLapDelta = true, lapDeltaUnavailable = false)
        try {
            val lapDelta = lapDeltaUseCase(sessionKey, fastest1, fastest2)
            _uiState.value = _uiState.value.copy(
                lapDelta = lapDelta, isComputingLapDelta = false, lapDeltaUnavailable = lapDelta == null
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // The lap-by-lap comparison is still useful without position data
            _uiState.value = _uiState.value.copy(isComputingLapDelta = false, lapDeltaUnavailable = true)
        }
    }
}
