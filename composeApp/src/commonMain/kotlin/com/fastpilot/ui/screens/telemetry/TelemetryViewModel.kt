package com.fastpilot.ui.screens.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.*
import com.fastpilot.model.CarData
import com.fastpilot.model.Driver
import com.fastpilot.model.Lap
import com.fastpilot.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TelemetryUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val drivers: List<Driver> = emptyList(),
    val selectedDriver: Driver? = null,
    val laps: List<Lap> = emptyList(),
    val selectedLap: Lap? = null,
    val carData: List<CarData> = emptyList(),
    val isLoadingTelemetry: Boolean = false,
    val error: String? = null
)

class TelemetryViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val lapRepo: LapRepository,
    private val telemetryRepo: TelemetryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init { loadSessions() }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val s2024 = try { sessionRepo.getSessions(2024) } catch (_: Exception) { emptyList() }
                val s2025 = try { sessionRepo.getSessions(2025) } catch (_: Exception) { emptyList() }
                val all = s2025 + s2024
                _uiState.value = _uiState.value.copy(sessions = all, isLoading = false)
                // Auto-select first qualifying or race session
                val auto = all.firstOrNull { it.sessionType == "Qualifying" || it.sessionType == "Race" }
                    ?: all.firstOrNull()
                auto?.let { selectSession(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectSession(session: Session) {
        _uiState.value = _uiState.value.copy(
            selectedSession = session, isLoading = true,
            drivers = emptyList(), selectedDriver = null,
            laps = emptyList(), selectedLap = null, carData = emptyList(), error = null
        )
        viewModelScope.launch {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                _uiState.value = _uiState.value.copy(drivers = drivers, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erro ao carregar pilotos: ${e.message}")
            }
        }
    }

    fun selectDriver(driver: Driver) {
        val sessionKey = _uiState.value.selectedSession?.sessionKey ?: return
        _uiState.value = _uiState.value.copy(
            selectedDriver = driver, laps = emptyList(),
            selectedLap = null, carData = emptyList(), isLoading = true
        )
        viewModelScope.launch {
            try {
                val laps = lapRepo.getLaps(sessionKey, driver.driverNumber)
                val validLaps = laps.filter { lap -> val d = lap.lapDuration; d != null && d > 0 }
                _uiState.value = _uiState.value.copy(laps = validLaps, isLoading = false)
                // Auto-select the fastest lap
                val fastest = validLaps.minByOrNull { it.lapDuration ?: Double.MAX_VALUE }
                fastest?.let { selectLap(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erro ao carregar voltas: ${e.message}")
            }
        }
    }

    fun selectLap(lap: Lap) {
        val sessionKey = _uiState.value.selectedSession?.sessionKey ?: return
        val driverNumber = _uiState.value.selectedDriver?.driverNumber ?: return
        _uiState.value = _uiState.value.copy(selectedLap = lap, isLoadingTelemetry = true, carData = emptyList())
        viewModelScope.launch {
            try {
                val data = telemetryRepo.getCarData(
                    sessionKey = sessionKey,
                    driverNumber = driverNumber,
                    dateGte = lap.dateStart
                )
                _uiState.value = _uiState.value.copy(carData = data.take(500), isLoadingTelemetry = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingTelemetry = false,
                    error = "Erro ao carregar telemetria: ${e.message}"
                )
            }
        }
    }
}
