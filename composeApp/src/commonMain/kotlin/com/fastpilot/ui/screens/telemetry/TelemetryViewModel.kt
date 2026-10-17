package com.fastpilot.ui.screens.telemetry

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.*
import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.domain.usecase.CalculateTheoreticalBestUseCase
import com.fastpilot.domain.usecase.CalculateTheoreticalBestUseCase.TheoreticalBest
import com.fastpilot.model.CarData
import com.fastpilot.model.Driver
import com.fastpilot.model.Lap
import com.fastpilot.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.fastpilot.ui.ScreenDispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class TelemetryUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val drivers: List<Driver> = emptyList(),
    val selectedDriver: Driver? = null,
    val laps: List<Lap> = emptyList(),
    val selectedLap: Lap? = null,
    /** Sum of the driver's best sectors in the session. */
    val theoreticalBest: TheoreticalBest? = null,
    val carData: List<CarData> = emptyList(),
    val isLoadingTelemetry: Boolean = false,
    val error: UiError? = null
)

class TelemetryViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val lapRepo: LapRepository,
    private val telemetryRepo: TelemetryRepository,
    private val theoreticalBestUseCase: CalculateTheoreticalBestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init { loadSessions() }

    // Picking the session (and the jobs it starts) stays on the main thread; the loading runs in the background
    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val all = withContext(ScreenDispatchers.background) { sessionRepo.getRecentSessions() }
                _uiState.update { it.copy(sessions = all, isLoading = false) }
                // Auto-select the live session, else the latest qualifying or race
                val auto = LiveSessionDetector.findLiveSession(all)
                    ?: all.firstOrNull { it.sessionType == "Qualifying" || it.sessionType == "Race" }
                    ?: all.firstOrNull()
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
            drivers = emptyList(), selectedDriver = null,
            laps = emptyList(), selectedLap = null, theoreticalBest = null, carData = emptyList(), error = null
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

    fun selectDriver(driver: Driver) {
        val sessionKey = _uiState.value.selectedSession?.sessionKey ?: return
        _uiState.update { it.copy(
            selectedDriver = driver, laps = emptyList(),
            selectedLap = null, theoreticalBest = null, carData = emptyList(), isLoading = true
        ) }
        viewModelScope.launch(ScreenDispatchers.background) {
            try {
                val laps = lapRepo.getLaps(sessionKey, driver.driverNumber)
                val validLaps = laps.filter { lap -> val d = lap.lapDuration; d != null && d > 0 }
                _uiState.update { it.copy(
                    laps = validLaps,
                    theoreticalBest = theoreticalBestUseCase.invoke(laps, driver.driverNumber),
                    isLoading = false
                ) }
                // Auto-select the fastest lap
                val fastest = validLaps.minByOrNull { it.lapDuration ?: Double.MAX_VALUE }
                fastest?.let { selectLap(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = UiError(ErrorKind.LAPS, e.message)) }
            }
        }
    }

    fun selectLap(lap: Lap) {
        val sessionKey = _uiState.value.selectedSession?.sessionKey ?: return
        if (_uiState.value.selectedDriver == null) return
        _uiState.update { it.copy(selectedLap = lap, isLoadingTelemetry = true, carData = emptyList()) }
        viewModelScope.launch(ScreenDispatchers.background) {
            try {
                // Only this lap's samples (cached for finished sessions)
                val data = telemetryRepo.getLapCarData(sessionKey, lap)
                _uiState.update { it.copy(carData = data.take(500), isLoadingTelemetry = false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoadingTelemetry = false,
                    error = UiError(ErrorKind.TELEMETRY, e.message)
                ) }
            }
        }
    }
}
