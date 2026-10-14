package com.fastpilot.ui.screens.standings

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.repository.StandingsRepository
import com.fastpilot.data.repository.availableSeasons
import com.fastpilot.domain.usecase.CalculateChampionshipGapsUseCase
import com.fastpilot.domain.usecase.ChampionshipTables
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StandingsMode { DRIVERS, CONSTRUCTORS }

data class StandingsUiState(
    val isLoading: Boolean = true,
    val seasons: List<Int> = StandingsViewModel.SEASONS,
    val selectedSeason: Int = StandingsViewModel.SEASONS.first(),
    val mode: StandingsMode = StandingsMode.DRIVERS,
    val tables: ChampionshipTables? = null,
    val error: UiError? = null
)

class StandingsViewModel(
    private val standingsRepo: StandingsRepository,
    private val calculateGaps: CalculateChampionshipGapsUseCase
) : ViewModel() {

    companion object {
        /** Seasons with OpenF1 coverage (2023 on), newest first, up to the current one. */
        val SEASONS = availableSeasons()
    }

    private val _uiState = MutableStateFlow(StandingsUiState())
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init { selectSeason(SEASONS.first()) }

    fun selectSeason(season: Int, forceRefresh: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            selectedSeason = season, isLoading = true, tables = null, error = null
        )
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val standings = standingsRepo.getStandings(season, forceRefresh)
                _uiState.value = _uiState.value.copy(
                    tables = calculateGaps(standings),
                    isLoading = false
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError(ErrorKind.STANDINGS, e.message)
                )
            }
        }
    }

    fun refresh() = selectSeason(_uiState.value.selectedSeason, forceRefresh = true)

    fun selectMode(mode: StandingsMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }
}
