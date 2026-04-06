package com.fastpilot.ui.screens.strategy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.*
import com.fastpilot.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StrategyUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val drivers: List<Driver> = emptyList(),
    val stintsByDriver: Map<Int, List<Stint>> = emptyMap(),
    val pitStops: List<PitStop> = emptyList(),
    val error: String? = null
)

class StrategyViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val stintRepo: StintRepository,
    private val pitStopRepo: PitStopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrategyUiState())
    val uiState: StateFlow<StrategyUiState> = _uiState.asStateFlow()

    init { loadSessions() }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val s2024 = try { sessionRepo.getSessions(2024) } catch (_: Exception) { emptyList() }
                val s2025 = try { sessionRepo.getSessions(2025) } catch (_: Exception) { emptyList() }
                val all = s2025 + s2024
                _uiState.value = _uiState.value.copy(sessions = all, isLoading = false)
                val auto = all.firstOrNull { it.sessionType == "Race" } ?: all.firstOrNull()
                auto?.let { selectSession(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectSession(session: Session) {
        _uiState.value = _uiState.value.copy(
            selectedSession = session, isLoading = true,
            stintsByDriver = emptyMap(), pitStops = emptyList(), error = null
        )
        viewModelScope.launch {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                val allStints = stintRepo.getAllStints(session.sessionKey)
                val pitStops = try { pitStopRepo.getPitStops(session.sessionKey) } catch (_: Exception) { emptyList() }
                _uiState.value = _uiState.value.copy(
                    drivers = drivers,
                    stintsByDriver = allStints.groupBy { it.driverNumber },
                    pitStops = pitStops,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erro: ${e.message}")
            }
        }
    }
}
