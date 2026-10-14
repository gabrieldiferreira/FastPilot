package com.fastpilot.ui.perf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fastpilot.data.polling.LivePollingManager
import com.fastpilot.data.repository.SessionRepository
import com.fastpilot.data.repository.defaultSession
import com.fastpilot.data.repository.getRecentSessions
import com.fastpilot.db.FastPilotDB
import com.fastpilot.network.OpenF1Api
import com.fastpilot.network.RequestStats
import com.fastpilot.network.clearPlatformHttpCache
import com.fastpilot.ui.screens.dashboard.DashboardViewModel
import com.fastpilot.ui.screens.duel.DuelViewModel
import com.fastpilot.ui.screens.radio.RadioViewModel
import com.fastpilot.ui.screens.standings.StandingsViewModel
import com.fastpilot.ui.screens.strategy.StrategyViewModel
import com.fastpilot.ui.screens.telemetry.TelemetryViewModel
import com.fastpilot.ui.screens.weather.WeatherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import org.koin.core.Koin
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * Screen load-time benchmark (TCC I §4.8): each screen is "opened" by creating its ViewModel,
 * exactly as navigation does, and timed until the state that renders its content.
 * Every screen is measured without cache (local database and platform HTTP cache cleared) and
 * right after with the local database cache,
 * first with no background fetching and then with the Dashboard's live polling running.
 *
 * Results are printed as `FASTPILOT_PERF {json}` lines, collected by `scripts/perf_run.py`.
 */
class LoadBenchmark(
    private val platform: String,
    private val koin: Koin,
    private val config: Config
) {
    data class Config(
        val runsWithoutPolling: Int = 5,
        val runsWithPolling: Int = 3,
        /** Pause before each measurement so the ~30 requests/minute OpenF1 budget recovers. */
        val cooldown: Duration = 15.seconds,
        val timeout: Duration = 90.seconds,
        /** Idle periods used as resource baselines (no loads; with and without polling). */
        val idle: Duration = 20.seconds
    ) {
        companion object {
            /** "off:5,on:3,cooldown:15,timeout:90,idle:20"; missing keys keep the defaults. */
            fun parse(spec: String?): Config {
                val values = spec.orEmpty().split(',').mapNotNull { entry ->
                    val (key, value) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                    value.trim().toIntOrNull()?.let { key.trim() to it }
                }.toMap()
                val default = Config()
                return Config(
                    runsWithoutPolling = values["off"] ?: default.runsWithoutPolling,
                    runsWithPolling = values["on"] ?: default.runsWithPolling,
                    cooldown = values["cooldown"]?.seconds ?: default.cooldown,
                    timeout = values["timeout"]?.seconds ?: default.timeout,
                    idle = values["idle"]?.seconds ?: default.idle
                )
            }
        }
    }

    private enum class Status { OK, ERROR, TIMEOUT }

    /**
     * One timed step after the screen opened: [trigger] is the user action that starts it, or null
     * when the screen keeps loading by itself and the step is timed from the opening.
     */
    private class Step<S>(
        val name: String,
        val trigger: (() -> Unit)?,
        val done: (S) -> Boolean,
        val ok: (S) -> Boolean
    )

    private class Screen<VM : ViewModel, S>(
        val name: String,
        val type: KClass<VM>,
        val state: (VM) -> StateFlow<S>,
        val loaded: (S) -> Boolean,
        val ok: (S) -> Boolean,
        val actions: (VM) -> List<Step<S>> = { emptyList() }
    )

    private val db: FastPilotDB? = koin.getOrNull()
    private val _progress = MutableStateFlow("Preparando...")
    val progress: StateFlow<String> = _progress.asStateFlow()

    private val screens: List<Screen<*, *>> = listOf(
        Screen(
            "dashboard", DashboardViewModel::class, { it.uiState },
            loaded = { it.positions.isNotEmpty() || it.error != null },
            ok = { it.positions.isNotEmpty() }
        ),
        Screen(
            "telemetry", TelemetryViewModel::class, { it.uiState },
            loaded = { it.drivers.isNotEmpty() || it.error != null },
            ok = { it.drivers.isNotEmpty() },
            actions = { vm ->
                listOf(Step(
                    "lap",
                    trigger = { vm.selectDriver(vm.uiState.value.drivers.first()) },
                    done = { it.carData.isNotEmpty() || it.error != null || (!it.isLoading && it.laps.isEmpty()) },
                    ok = { it.carData.isNotEmpty() }
                ))
            }
        ),
        Screen(
            "strategy", StrategyViewModel::class, { it.uiState },
            loaded = { (it.selectedSession != null && !it.isLoading) || it.error != null },
            ok = { it.stintsByDriver.isNotEmpty() },
            actions = { _ ->
                // Clean pace and degradation keep loading after the stints are shown;
                // timed from the screen opening since there is no user action
                listOf(Step(
                    "metrics",
                    trigger = null,
                    done = { it.cleanPace.isNotEmpty() || it.error != null || (!it.isLoading && !it.isLoadingMetrics && it.stintsByDriver.isEmpty()) },
                    ok = { it.cleanPace.isNotEmpty() }
                ))
            }
        ),
        Screen(
            "duel", DuelViewModel::class, { it.uiState },
            loaded = { it.drivers.isNotEmpty() || it.error != null },
            ok = { it.drivers.isNotEmpty() },
            actions = { vm ->
                listOf(Step(
                    "compare",
                    trigger = {
                        val drivers = vm.uiState.value.drivers
                        vm.selectDriver1(drivers[0])
                        vm.selectDriver2(drivers[1])
                    },
                    done = { (!it.isComputing && !it.isComputingLapDelta && (it.lapDelta != null || it.lapDeltaUnavailable)) || it.error != null },
                    ok = { it.lapDelta != null }
                ))
            }
        ),
        Screen(
            "radio", RadioViewModel::class, { it.uiState },
            loaded = { (it.selectedSession != null && !it.isLoading) || it.error != null },
            ok = { it.radios.isNotEmpty() }
        ),
        Screen(
            "weather", WeatherViewModel::class, { it.uiState },
            loaded = { (it.selectedSession != null && !it.isLoading) || it.error != null },
            ok = { it.currentWeather != null }
        ),
        Screen(
            "standings", StandingsViewModel::class, { it.uiState },
            loaded = { !it.isLoading || it.error != null },
            ok = { it.tables != null }
        )
    )

    suspend fun run(scope: CoroutineScope) {
        log(
            """{"type":"start","platform":"$platform","cache":${db != null},"runsOff":${config.runsWithoutPolling},""" +
                """"runsOn":${config.runsWithPolling},"cooldownS":${config.cooldown.inWholeSeconds},"t":${now()}}"""
        )

        phase("idle")
        _progress.value = "Linha de base (ocioso)"
        delay(config.idle)

        phase("loads_polling_off")
        repeat(config.runsWithoutPolling) { runAll(it + 1, polling = false) }

        // "on:0" skips the polling part: on the free OpenF1 tier, live polling alone (~46 requests
        // per minute) exceeds the 30 per minute quota, so loads would only measure the quota
        if (config.runsWithPolling > 0) {
            // Same session the screens select by default: the most recent race
            val sessionKey = koin.get<SessionRepository>().getRecentSessions().defaultSession()?.sessionKey
            val polling = sessionKey?.let { startPolling(scope, it) }

            phase("idle_polling_on")
            _progress.value = "Polling ativo (ocioso)"
            delay(config.idle)

            phase("loads_polling_on")
            repeat(config.runsWithPolling) { runAll(it + 1, polling = true) }

            polling?.cancel()
        }
        phase("done")
        _progress.value = "Concluído"
    }

    /**
     * The Dashboard's live mode on a past race: positions and intervals every 4 s, race control
     * every 10 s, pit stops and overtakes every 15 s, weather every 60 s. The API is called
     * directly because the repositories answer a finished race from the local cache.
     */
    private fun startPolling(scope: CoroutineScope, sessionKey: Int): Job {
        val api = koin.get<OpenF1Api>()
        fun CoroutineScope.every(periodMs: Long, request: suspend () -> Unit) = launch {
            while (true) {
                try { request() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
                delay(periodMs)
            }
        }
        return scope.launch {
            every(LivePollingManager.POSITION_POLL_MS) { api.getPositions(sessionKey = sessionKey) }
            every(LivePollingManager.POSITION_POLL_MS) { api.getIntervals(sessionKey) }
            every(LivePollingManager.RACE_CONTROL_POLL_MS) { api.getRaceControl(sessionKey = sessionKey) }
            every(LivePollingManager.RACE_EVENTS_POLL_MS) { api.getPitStops(sessionKey); api.getOvertakes(sessionKey) }
            every(LivePollingManager.WEATHER_POLL_MS) { api.getWeather(sessionKey = sessionKey) }
        }
    }

    private suspend fun runAll(run: Int, polling: Boolean) {
        for (screen in screens) {
            // Without a local database (Web) there is no "with cache" case
            val caches = if (db != null) listOf("cold", "warm") else listOf("cold")
            for (cache in caches) {
                if (cache == "cold") {
                    db?.fastPilotDBQueries?.clearAll()
                    clearPlatformHttpCache()
                }
                _progress.value = "${if (polling) "Com" else "Sem"} polling • execução $run • ${screen.name} ($cache)"
                delay(config.cooldown)
                @Suppress("UNCHECKED_CAST")
                measure(screen as Screen<ViewModel, Any?>, run, cache, polling)
            }
        }
    }

    private suspend fun <VM : ViewModel, S> measure(screen: Screen<VM, S>, run: Int, cache: String, polling: Boolean) {
        val store = ViewModelStore()
        try {
            var requests = RequestStats.requests
            var limited = RequestStats.rateLimited
            var opened = TimeSource.Monotonic.markNow()
            val factory = viewModelFactory { addInitializer(screen.type) { koin.get(screen.type) } }
            val vm = ViewModelProvider.create(store, factory)[screen.type]
            val state = screen.state(vm)
            val loaded = withTimeoutOrNull(config.timeout) { state.first(screen.loaded) }
            record(screen.name, "open", run, cache, polling, opened.elapsedNow(), status(loaded, screen.ok), requests, limited)
            if (loaded == null || !screen.ok(loaded)) return

            for (step in screen.actions(vm)) {
                step.trigger?.let { trigger ->
                    opened = TimeSource.Monotonic.markNow()
                    requests = RequestStats.requests
                    limited = RequestStats.rateLimited
                    trigger()
                }
                val done = withTimeoutOrNull(config.timeout) { state.first(step.done) }
                record(screen.name, step.name, run, cache, polling, opened.elapsedNow(), status(done, step.ok), requests, limited)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            log("""{"type":"failure","screen":"${screen.name}","run":$run,"cache":"$cache","polling":$polling,"message":"${e.message.orEmpty().replace("\"", "'")}"}""")
        } finally {
            store.clear()
        }
    }

    private fun <S> status(state: S?, ok: (S) -> Boolean) = when {
        state == null -> Status.TIMEOUT
        ok(state) -> Status.OK
        else -> Status.ERROR
    }

    private fun record(
        screen: String, step: String, run: Int, cache: String, polling: Boolean,
        elapsed: Duration, status: Status, requestsBefore: Int, limitedBefore: Int
    ) {
        log(
            """{"type":"load","platform":"$platform","screen":"$screen","step":"$step","run":$run,"cache":"$cache",""" +
                """"polling":$polling,"ms":${elapsed.toDouble(DurationUnit.MILLISECONDS)},"status":"${status.name.lowercase()}",""" +
                """"requests":${RequestStats.requests - requestsBefore},"rateLimited":${RequestStats.rateLimited - limitedBefore},"t":${now()}}"""
        )
    }

    private fun phase(name: String) = log("""{"type":"phase","phase":"$name","t":${now()}}""")

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private fun log(json: String) = println("FASTPILOT_PERF $json")
}
