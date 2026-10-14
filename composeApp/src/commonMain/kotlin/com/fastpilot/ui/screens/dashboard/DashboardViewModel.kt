package com.fastpilot.ui.screens.dashboard

import com.fastpilot.ui.i18n.ErrorKind
import com.fastpilot.ui.i18n.UiError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastpilot.data.polling.LivePollingManager
import com.fastpilot.data.repository.*
import com.fastpilot.domain.usecase.GetLivePositionsUseCase
import com.fastpilot.domain.usecase.GetRaceEventsUseCase
import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.domain.usecase.PositionWithDriver
import com.fastpilot.domain.usecase.RaceEvent
import com.fastpilot.model.Interval
import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Session
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isLive: Boolean = false,
    val currentSession: Session? = null,
    val sessions: List<Session> = emptyList(),
    val positions: List<PositionWithDriver> = emptyList(),
    val intervals: Map<Int, Interval> = emptyMap(),
    /** Every driver's laps; only for practice and qualifying, which have no intervals. */
    val laps: List<Lap> = emptyList(),
    val raceControlMessages: List<RaceControlMessage> = emptyList(),
    val events: List<RaceEvent> = emptyList(),
    val error: UiError? = null
)

class DashboardViewModel(
    private val sessionRepo: SessionRepository,
    private val getPositions: GetLivePositionsUseCase,
    private val intervalRepo: IntervalRepository,
    private val raceControlRepo: RaceControlRepository,
    private val livePolling: LivePollingManager,
    private val getRaceEvents: GetRaceEventsUseCase,
    private val lapRepo: LapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null
    private var intervalPollingJob: Job? = null
    private var raceControlPollingJob: Job? = null
    private var eventsJob: Job? = null
    private var lapsJob: Job? = null
    private var liveWatchJob: Job? = null

    /** The whole calendar of the recent seasons, including sessions that have not started yet. */
    private var seasonSessions: List<Session> = emptyList()
    private var userPickedSession = false

    companion object {
        /** How often the dashboard checks whether a session has started or ended. */
        private const val LIVE_CHECK_MS = 30_000L

        /** How long the secondary requests wait for the first positions. */
        private const val POSITIONS_FIRST_MS = 5_000L
    }

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                seasonSessions = sessionRepo.getSeasonSessions()
                val allSessions = seasonSessions.startedBy()

                // Default: live session > most recent race > newest session
                val defaultSession = allSessions.defaultSession()
                if (defaultSession == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UiError(ErrorKind.NO_SESSIONS)
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(sessions = allSessions, isLoading = false)
                showSession(defaultSession)
                watchLiveSessions()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError(ErrorKind.SESSIONS, e.message)
                )
            }
        }
    }

    /**
     * Keeps the dashboard in step with the calendar while it is open: a session that starts is
     * listed and, unless the user picked another one, opened live; a live session that ends
     * stops polling and is reloaded as historical data.
     */
    private fun watchLiveSessions() {
        liveWatchJob?.cancel()
        liveWatchJob = viewModelScope.launch {
            while (true) {
                delay(LIVE_CHECK_MS)
                val now = Clock.System.now()
                val started = seasonSessions.startedBy(now)
                if (started.size != _uiState.value.sessions.size) {
                    _uiState.value = _uiState.value.copy(sessions = started)
                }
                val state = _uiState.value
                val current = state.currentSession ?: continue
                val live = LiveSessionDetector.findLiveSession(started, now)
                when {
                    live != null && !userPickedSession && live.sessionKey != current.sessionKey -> showSession(live)
                    state.isLive && !LiveSessionDetector.isLive(current, now) -> showSession(current)
                }
            }
        }
    }

    fun selectSession(session: Session) {
        userPickedSession = true
        showSession(session)
    }

    private fun showSession(session: Session) {
        val isLive = LiveSessionDetector.isLive(session)
        _uiState.value = _uiState.value.copy(
            currentSession = session,
            isLive = isLive,
            positions = emptyList(),
            intervals = emptyMap(),
            laps = emptyList(),
            raceControlMessages = emptyList(),
            events = emptyList(),
            isLoading = true,
            error = null
        )
        loadSessionData(session)
    }

    private fun loadSessionData(session: Session) {
        // Cancel previous polling jobs
        pollingJob?.cancel()
        intervalPollingJob?.cancel()
        raceControlPollingJob?.cancel()
        eventsJob?.cancel()
        lapsJob?.cancel()

        val sessionKey = session.sessionKey
        val isLive = LiveSessionDetector.isLive(session)

        // OpenF1 only has intervals for races and sprints: practice and qualifying show lap times
        if (!session.isRace()) {
            lapsJob = viewModelScope.launch {
                while (true) {
                    // Fetched before reading the state: copying a state read before the request would
                    // undo whatever arrived meanwhile (race control, intervals...)
                    val laps = lapRepo.getAllLaps(sessionKey)
                    _uiState.value = _uiState.value.copy(laps = laps)
                    if (!isLive) break
                    delay(LivePollingManager.RACE_EVENTS_POLL_MS)
                }
            }
        }

        // The requests share the OpenF1 rate limit: positions (what the spinner waits for) go
        // first, the rest waits for them (or at most a few seconds, if they fail)
        val positionsShown = CompletableDeferred<Unit>()
        suspend fun afterPositions() { withTimeoutOrNull(POSITIONS_FIRST_MS) { positionsShown.await() } }

        if (isLive) {
            // ── LIVE MODE: continuous polling ───────────────────────
            pollingJob = viewModelScope.launch {
                getPositions.invoke(sessionKey, pollIntervalMs = LivePollingManager.POSITION_POLL_MS).collect { positions ->
                    _uiState.value = _uiState.value.copy(positions = positions, isLoading = false)
                    positionsShown.complete(Unit)
                }
            }

            intervalPollingJob = viewModelScope.launch {
                afterPositions()
                livePolling.observeIntervals(sessionKey).collect { intervals ->
                    _uiState.value = _uiState.value.copy(
                        intervals = intervals.associateBy { it.driverNumber }
                    )
                }
            }

            raceControlPollingJob = viewModelScope.launch {
                afterPositions()
                livePolling.observeRaceControl(sessionKey).collect { messages ->
                    _uiState.value = _uiState.value.copy(raceControlMessages = messages)
                }
            }

            eventsJob = viewModelScope.launch {
                afterPositions()
                getRaceEvents.observe(sessionKey, LivePollingManager.RACE_EVENTS_POLL_MS).collect { events ->
                    _uiState.value = _uiState.value.copy(events = events)
                }
            }
        } else {
            // ── HISTORICAL MODE: one-shot fetch ────────────────────
            pollingJob = viewModelScope.launch {
                try {
                    // Once: without network and without the session in the cache this fails right away
                    // and the error is shown (a flow that retries would keep the spinner forever)
                    val positions = getPositions.once(sessionKey)
                    _uiState.value = _uiState.value.copy(positions = positions, isLoading = false)
                    positionsShown.complete(Unit)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    positionsShown.complete(Unit)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UiError(ErrorKind.POSITIONS, e.message)
                    )
                }
            }

            // Intervals (one-shot); kept in a job so that switching sessions cancels it
            intervalPollingJob = viewModelScope.launch {
                afterPositions()
                try {
                    val intervals = intervalRepo.getLatestIntervals(sessionKey)
                    _uiState.value = _uiState.value.copy(
                        intervals = intervals.associateBy { it.driverNumber }
                    )
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
            }

            // Pit stops and overtakes (one-shot)
            eventsJob = viewModelScope.launch {
                afterPositions()
                val events = getRaceEvents(sessionKey)
                _uiState.value = _uiState.value.copy(events = events)
            }

            // Race control (one-shot)
            raceControlPollingJob = viewModelScope.launch {
                afterPositions()
                try {
                    val messages = raceControlRepo.getRaceControl(sessionKey)
                    _uiState.value = _uiState.value.copy(raceControlMessages = messages)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
            }
        }
    }
}

/** Races and sprints (OpenF1 session_type "Race"). */
fun Session.isRace() = sessionType == "Race"

