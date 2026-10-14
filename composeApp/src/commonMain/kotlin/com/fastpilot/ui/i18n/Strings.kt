package com.fastpilot.ui.i18n

/**
 * Every text the app shows, in one language. Each [AppLanguage] has its own implementation
 * (PtBrStrings, EnUsStrings, EsStrings), so a text missing in any language does not compile.
 * Data that comes from the APIs as text (race control messages, country and circuit names)
 * is shown as received.
 */
interface Strings {
    // ── Login ──────────────────────────────────────────────────────────
    val loginTagline: String
    val loginWithGoogle: String
    val loginAsGuest: String
    val language: String

    /** Web address of each screen in this language (no accents: they would show as %C3%A7 in the address bar). */
    val routes: Routes

    // ── Tabs ───────────────────────────────────────────────────────────
    val tabDashboard: String
    val tabTelemetry: String
    val tabStrategy: String
    val tabDuel: String
    val tabRadio: String
    val tabWeather: String
    val tabStandings: String

    // ── Shared ─────────────────────────────────────────────────────────
    /** ',' in Portuguese and Spanish, '.' in English (lap times keep '.' in every language). */
    val decimalSeparator: Char
    val loading: String
    val select: String
    val selectAnotherSession: String
    val selectYear: String
    val selectRaceWeekend: String
    val selectDriver: String
    val selectADriver: String
    val preSeasonTesting: String
    fun round(number: Int): String
    fun lap(number: Int): String
    /** OpenF1 gaps of lapped cars, "+1 LAP" / "+2 LAPS", in this language; any other gap as sent. */
    fun gap(apiGap: String): String
    /** "Horários no seu fuso (UTC−03:00)". */
    fun localTimes(offset: String): String
    /** Lap chip label: "V12", "L12". */
    fun lapShort(number: Int): String
    /** Short label of an OpenF1 session name ("Practice 1" -> "TL1"). */
    fun sessionShort(sessionName: String?): String
    /** Full label of an OpenF1 session name ("Practice 1" -> "Treino Livre 1"). */
    fun sessionLong(sessionName: String?): String
    fun error(error: UiError): String
    /** A country, circuit or city name from OpenF1 ("Mexico City" -> "Cidade do México"); unknown names as sent. */
    fun place(name: String?): String
    /** A race control message from the FIA, in English; see [translateRaceControl]. */
    fun raceControl(message: String): String
    val retry: String
    val refresh: String
    val yes: String
    val no: String

    // ── Dashboard ──────────────────────────────────────────────────────
    val loadingData: String
    val noPositions: String
    val live: String
    val historical: String
    val fastest: String
    val noTime: String
    val events: String
    val raceControl: String
    fun pitStop(driver: String): String
    fun stationary(seconds: String): String
    fun pitLane(seconds: String): String
    fun overtake(driver: String, overtaken: String): String
    val overtakeBadge: String

    // ── Telemetry ──────────────────────────────────────────────────────
    val telemetryTitle: String
    val driver: String
    val noDriversForSession: String
    val laps: String
    fun noLapsFor(driver: String?): String
    val loadingTelemetry: String
    val speedChart: String
    val throttleBrakeChart: String
    val throttle: String
    val brake: String
    val gear: String
    val selectDriverAbove: String
    val toSeeTelemetry: String
    val lapLabel: String
    val timeLabel: String
    val theoreticalLap: String
    val theoreticalLapHint: String
    val theoretical: String
    val bestActualLap: String
    val difference: String

    // ── Strategy ───────────────────────────────────────────────────────
    val strategyTitle: String
    val loadingStrategy: String
    val noStints: String
    val selectARace: String
    val degradationHint: String
    val pitStops: String
    val perLap: String
    val cleanPace: String
    val cleanPaceHint: String
    val raceControlUnavailable: String
    fun cleanLaps(clean: Int, total: Int): String
    val reference: String
    val pitStopLabel: String

    // ── Duel ───────────────────────────────────────────────────────────
    val duelTitle: String
    fun driverN(n: Int): String
    val selectTwoDrivers: String
    val toComparePerformance: String
    val computingComparison: String
    val cumulativeDelta: String
    fun positiveMeansAhead(driver: String): String
    val averagePace: String
    val bestLap: String
    val validLaps: String
    val noCommonLaps: String
    val fastestLapDelta: String
    val positionDataUnavailable: String
    fun lapVsLap(lap1: Int, driver1: String, lap2: Int, driver2: String): String
    fun fasterBy(driver: String, gap: String): String
    val sameLapTime: String

    // ── Radio ──────────────────────────────────────────────────────────
    val radioTitle: String
    val all: String
    val loadingRadios: String
    val noRadios: String
    val noRadiosForSession: String
    fun radioCount(count: Int): String
    val pause: String
    val listen: String
    val playing: String
    val listenInApp: String
    val audioUnavailable: String

    // ── Weather ────────────────────────────────────────────────────────
    val weatherTitle: String
    val loadingWeather: String
    val wetConditions: String
    val dryTrack: String
    val airTemp: String
    val trackTemp: String
    val humidity: String
    val rain: String
    val wind: String
    val pressure: String
    val sessionHistory: String
    fun readings(count: Int): String
    val trackPrefix: String
    val windPrefix: String
    val noWeather: String
    val windDirection: String

    // ── Standings ──────────────────────────────────────────────────────
    val standingsTitle: String
    fun seasonAfterRound(season: Int, round: Int): String
    fun season(season: Int): String
    val drivers: String
    val constructors: String
    val loadingStandings: String
    val noStandings: String
    val wins: String
    val leader: String
}

/** One path per screen ("/telemetria"); see `UrlRouter`. */
data class Routes(
    val login: String,
    val dashboard: String,
    val telemetry: String,
    val strategy: String,
    val duel: String,
    val radio: String,
    val weather: String,
    val standings: String,
)

/** What failed to load; the screen turns it into text with [Strings.error]. */
enum class ErrorKind { NO_SESSIONS, SESSIONS, POSITIONS, DRIVERS, LAPS, TELEMETRY, RADIO, WEATHER, STANDINGS, DUEL, GENERIC }

/** A failure reported by a ViewModel: [kind] is translated, [detail] (the exception message) is not. */
data class UiError(val kind: ErrorKind, val detail: String? = null)

/** OpenF1 session names ("Practice 1", "Sprint Qualifying", "Day 2"...) by kind, for the translations. */
internal enum class SessionKind { PRACTICE, TESTING_DAY, SPRINT_QUALIFYING, SPRINT, QUALIFYING, RACE }

/** The kind of an OpenF1 session name and its number (practice 1-3, testing day 1-3). */
internal fun sessionKind(sessionName: String): Pair<SessionKind, String>? = when {
    sessionName.startsWith("Practice ") -> SessionKind.PRACTICE to sessionName.removePrefix("Practice ")
    sessionName.startsWith("Day ") -> SessionKind.TESTING_DAY to sessionName.removePrefix("Day ")
    // "Sprint Shootout" in 2023
    sessionName == "Sprint Qualifying" || sessionName == "Sprint Shootout" -> SessionKind.SPRINT_QUALIFYING to ""
    sessionName == "Sprint" -> SessionKind.SPRINT to ""
    sessionName == "Qualifying" -> SessionKind.QUALIFYING to ""
    sessionName == "Race" -> SessionKind.RACE to ""
    else -> null
}

/** "+1 LAP", "+3 LAPS" -> 1, 3; null for a time gap. */
internal fun lapsDown(apiGap: String): Int? =
    Regex("""\+(\d+) LAPS?""").matchEntire(apiGap.trim())?.groupValues?.get(1)?.toInt()

/** 2.43 -> "2,4" (or "2.4" with [decimalSeparator] '.'). */
internal fun oneDecimal(value: Double, decimalSeparator: Char): String {
    val tenths = kotlin.math.round(value * 10).toInt()
    return "${tenths / 10}$decimalSeparator${kotlin.math.abs(tenths % 10)}"
}
