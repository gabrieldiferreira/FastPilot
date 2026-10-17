package com.fastpilot.ui.screens.strategy

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.*
import com.fastpilot.domain.usecase.CalculateCleanPaceUseCase
import com.fastpilot.domain.usecase.CalculateCleanPaceUseCase.CleanPaceStats
import com.fastpilot.domain.usecase.GetTyreDegradationUseCase
import com.fastpilot.domain.usecase.GetTyreDegradationUseCase.DegradationCurve
import com.fastpilot.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.fastpilot.ui.ScreenDispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class StrategyUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val drivers: List<Driver> = emptyList(),
    val stintsByDriver: Map<Int, List<Stint>> = emptyMap(),
    val pitStops: List<PitStop> = emptyList(),
    /** Drivers with enough clean laps, fastest median pace first (TCC I §4.7). */
    val cleanPace: List<CleanPaceStats> = emptyList(),
    /** Tyre degradation (s/lap) per stint, by driver number. */
    val degradationByDriver: Map<Int, List<DegradationCurve>> = emptyMap(),
    val isLoadingMetrics: Boolean = false,
    /** /race_control could not be loaded: safety car laps may be included in the metrics. */
    val metricsWithoutRaceControl: Boolean = false,
    val error: UiError? = null
)

/** Fewer clean laps than this (e.g. an early retirement) do not give a meaningful pace. */
internal const val MIN_CLEAN_LAPS = 5

class StrategyViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val stintRepo: StintRepository,
    private val pitStopRepo: PitStopRepository,
    private val lapRepo: LapRepository,
    private val raceControlRepo: RaceControlRepository,
    private val cleanPaceUseCase: CalculateCleanPaceUseCase,
    private val degradationUseCase: GetTyreDegradationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrategyUiState())
    val uiState: StateFlow<StrategyUiState> = _uiState.asStateFlow()

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
            stintsByDriver = emptyMap(), pitStops = emptyList(),
            cleanPace = emptyList(), degradationByDriver = emptyMap(),
            isLoadingMetrics = false, metricsWithoutRaceControl = false, error = null
        ) }
        viewModelScope.launch(ScreenDispatchers.background) {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                val allStints = stintRepo.getAllStints(session.sessionKey)
                val pitStops = try { pitStopRepo.getPitStops(session.sessionKey) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { emptyList() }
                _uiState.update { it.copy(
                    drivers = drivers,
                    stintsByDriver = allStints.groupBy { it.driverNumber },
                    pitStops = pitStops,
                    isLoading = false
                ) }
                if (session.sessionType == "Race") loadMetrics(session.sessionKey, allStints)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = UiError(ErrorKind.GENERIC, e.message)) }
            }
        }
    }

    /**
     * Clean pace and tyre degradation need every driver's laps (the leader's timing places
     * race control periods) and the safety car / VSC / red flag messages.
     */
    private suspend fun loadMetrics(sessionKey: Int, stints: List<Stint>) {
        _uiState.update { it.copy(isLoadingMetrics = true) }
        val laps = lapRepo.getAllLaps(sessionKey)
        val raceControl = try {
            raceControlRepo.getRaceControl(sessionKey)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val messages = raceControl.orEmpty()
        val drivers = laps.map { it.driverNumber }.distinct()

        val cleanPace = drivers
            .map { cleanPaceUseCase.invoke(laps, it, messages) }
            .filter { it.cleanLapCount >= MIN_CLEAN_LAPS }
            .sortedBy { it.medianPace }
        val degradation = drivers
            .associateWith { degradationUseCase.invoke(laps, stints, it, messages) }
            .filterValues { it.isNotEmpty() }

        // The user may have picked another session meanwhile
        if (_uiState.value.selectedSession?.sessionKey != sessionKey) return
        _uiState.update { it.copy(
            cleanPace = cleanPace,
            degradationByDriver = degradation,
            isLoadingMetrics = false,
            metricsWithoutRaceControl = raceControl == null && laps.isNotEmpty()
        ) }
    }
}
