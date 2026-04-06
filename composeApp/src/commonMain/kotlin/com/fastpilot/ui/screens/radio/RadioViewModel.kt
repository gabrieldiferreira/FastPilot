package com.fastpilot.ui.screens.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.TeamRadioRepository
import com.fastpilot.model.Driver
import com.fastpilot.model.Session
import com.fastpilot.model.TeamRadio
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
    val error: String? = null
)

class RadioViewModel(
    private val sessionRepo: SessionRepository,
    private val driverRepo: DriverRepository,
    private val radioRepo: TeamRadioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

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
            radios = emptyList(), selectedDriver = null, error = null
        )
        viewModelScope.launch {
            try {
                val drivers = driverRepo.getDriversBySession(session.sessionKey)
                val radios = radioRepo.getTeamRadio(session.sessionKey)
                _uiState.value = _uiState.value.copy(
                    drivers = drivers, radios = radios, isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = "Erro ao carregar rádios: ${e.message}"
                )
            }
        }
    }

    fun filterByDriver(driver: Driver?) {
        _uiState.value = _uiState.value.copy(selectedDriver = driver)
    }
}
