package com.fastpilot.ui.screens.duel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.SessionRepository
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
    val error: String? = null
)

class DuelViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val duelUseCase: GetDriverDuelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuelUiState())
    val uiState: StateFlow<DuelUiState> = _uiState.asStateFlow()

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
            drivers = emptyList(), driver1 = null, driver2 = null, duelData = null, error = null
        )
        viewModelScope.launch {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                _uiState.value = _uiState.value.copy(drivers = drivers, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectDriver1(driver: Driver) {
        _uiState.value = _uiState.value.copy(driver1 = driver, duelData = null)
        tryComputeDuel()
    }

    fun selectDriver2(driver: Driver) {
        _uiState.value = _uiState.value.copy(driver2 = driver, duelData = null)
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isComputing = false,
                    error = "Erro ao comparar pilotos: ${e.message}"
                )
            }
        }
    }
}
