package com.fastpilot.ui.screens.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.WeatherRepository
import com.fastpilot.model.Session
import com.fastpilot.model.Weather
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = true,
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val currentWeather: Weather? = null,
    val weatherHistory: List<Weather> = emptyList(),
    val error: String? = null
)

class WeatherViewModel(
    private val sessionRepo: SessionRepository,
    private val weatherRepo: WeatherRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

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
            currentWeather = null, weatherHistory = emptyList(), error = null
        )
        viewModelScope.launch {
            try {
                val history = weatherRepo.getWeather(session.sessionKey)
                _uiState.value = _uiState.value.copy(
                    weatherHistory = history,
                    currentWeather = history.lastOrNull(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar clima: ${e.message}"
                )
            }
        }
    }
}
