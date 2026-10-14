package com.fastpilot.data.repository

import com.fastpilot.model.CarData
import com.fastpilot.model.Interval
import com.fastpilot.model.Lap
import com.fastpilot.model.Location
import com.fastpilot.model.Position
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Weather
import com.fastpilot.model.TeamRadio
import com.fastpilot.model.PitStop
import com.fastpilot.model.Meeting
import com.fastpilot.model.Overtake
import com.fastpilot.db.FastPilotDB
import com.fastpilot.network.OpenF1Api
import com.fastpilot.network.SessionRegistry
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Repository for telemetry data (car_data endpoint).
 * A lap's samples of a finished session are kept in the local cache ([SamplesCache]).
 */
class TelemetryRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getCarData(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String? = null,
        dateLte: String? = null
    ): List<CarData> = api.getCarData(sessionKey, driverNumber, dateGte, dateLte)

    /** Samples of one lap (~270 at 3.7 Hz); without a lap time, everything from the lap start. */
    suspend fun getLapCarData(sessionKey: Int, lap: Lap): List<CarData> {
        val start = lap.dateStart ?: return emptyList()
        val end = lapEnd(lap) ?: return getCarData(sessionKey, lap.driverNumber, dateGte = start)
        return cache.getOrFetch("car_data", sessionKey, lap.driverNumber, start, end, ListSerializer(CarData.serializer())) {
            getCarData(sessionKey, lap.driverNumber, dateGte = start, dateLte = end)
        }
    }
}

/**
 * Repository for position data (polling-friendly).
 * The final order of a finished session is kept in the local cache ([SamplesCache]).
 */
class PositionRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getPositions(sessionKey: Int, driverNumber: Int? = null): List<Position> =
        api.getPositions(sessionKey = sessionKey, driverNumber = driverNumber)

    suspend fun getLatestPositions(sessionKey: Int): List<Position> =
        cache.getOrFetchSession("position_latest", sessionKey, ListSerializer(Position.serializer())) {
            val all = api.getPositions(sessionKey = sessionKey)
            // Group by driver, keep last position for each
            all.groupBy { it.driverNumber }
                .mapValues { (_, positions) -> positions.last() }
                .values
                .sortedBy { it.position }
        }
}

/**
 * Repository for interval/gap data (polling-friendly).
 */
class IntervalRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getIntervals(sessionKey: Int, driverNumber: Int? = null): List<Interval> =
        api.getIntervals(sessionKey, driverNumber)

    /** Last gap of each driver; only these are cached (a race has ~20 000 interval samples). */
    suspend fun getLatestIntervals(sessionKey: Int): List<Interval> =
        cache.getOrFetchSession("intervals_latest", sessionKey, ListSerializer(Interval.serializer())) {
            api.getIntervals(sessionKey).groupBy { it.driverNumber }
                .mapValues { (_, intervals) -> intervals.last() }
                .values
                .toList()
        }
}

/**
 * Repository for race control messages.
 */
class RaceControlRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getRaceControl(sessionKey: Int): List<RaceControlMessage> =
        cache.getOrFetchSession("race_control", sessionKey, ListSerializer(RaceControlMessage.serializer())) {
            api.getRaceControl(sessionKey = sessionKey)
        }

    suspend fun getFlags(sessionKey: Int): List<RaceControlMessage> =
        getRaceControl(sessionKey).filter { it.category == "Flag" }
}

/**
 * Repository for weather data.
 */
class WeatherRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getWeather(sessionKey: Int): List<Weather> =
        cache.getOrFetchSession("weather", sessionKey, ListSerializer(Weather.serializer())) {
            api.getWeather(sessionKey = sessionKey)
        }

    suspend fun getLatestWeather(sessionKey: Int): Weather? =
        getWeather(sessionKey).lastOrNull()
}

/**
 * Repository for team radio communications.
 */
class TeamRadioRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    /** The message list of a finished session is cached; the audio files always come from the network. */
    suspend fun getTeamRadio(sessionKey: Int, driverNumber: Int? = null): List<TeamRadio> =
        if (driverNumber != null) api.getTeamRadio(sessionKey, driverNumber)
        else cache.getOrFetchSession("team_radio", sessionKey, ListSerializer(TeamRadio.serializer())) {
            api.getTeamRadio(sessionKey, null)
        }
}

/**
 * Repository for pit stop data.
 */
class PitStopRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getPitStops(sessionKey: Int, driverNumber: Int? = null): List<PitStop> =
        if (driverNumber != null) api.getPitStops(sessionKey, driverNumber)
        else cache.getOrFetchSession("pit", sessionKey, ListSerializer(PitStop.serializer())) {
            api.getPitStops(sessionKey, null)
        }
}

/**
 * Repository for overtakes (passes between two cars).
 */
class OvertakeRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getOvertakes(sessionKey: Int): List<Overtake> =
        cache.getOrFetchSession("overtakes", sessionKey, ListSerializer(Overtake.serializer())) {
            api.getOvertakes(sessionKey)
        }
}

/**
 * Repository for meeting (Grand Prix) data.
 */
class MeetingRepository(private val api: OpenF1Api) {

    suspend fun getMeetings(year: Int): List<Meeting> =
        api.getMeetings(year = year)

    suspend fun getMeeting(meetingKey: Int): Meeting? =
        api.getMeetings(meetingKey = meetingKey).firstOrNull()
}

/**
 * Repository for car positions on track (location endpoint).
 * Only the samples of the requested lap are fetched (~350 per lap); those of a finished
 * session are kept in the local cache ([SamplesCache]).
 */
class LocationRepository(
    private val api: OpenF1Api,
    db: FastPilotDB? = null
) {
    private val cache = SamplesCache(db, api.sessionRegistry)

    suspend fun getLapLocations(sessionKey: Int, lap: Lap): List<Location> {
        val start = lap.dateStart ?: return emptyList()
        val end = lapEnd(lap) ?: return emptyList()
        return cache.getOrFetch("location", sessionKey, lap.driverNumber, start, end, ListSerializer(Location.serializer())) {
            api.getLocation(sessionKey, lap.driverNumber, dateGte = start, dateLte = end)
        }
    }
}

/** Driver number of whole-session entries in cached_samples (no car has number 0). */
private const val ALL_DRIVERS = 0

/** End of [lap] plus one second of margin, or null without a start or a lap time. */
private fun lapEnd(lap: Lap): String? {
    val start = lap.dateStart?.let(Instant::parse) ?: return null
    val duration = lap.lapDuration ?: return null
    return (start + (duration * 1000).toLong().milliseconds + 1.seconds).toString()
}

/**
 * Raw API data of a finished session, stored as JSON (TCC I §4.5): the samples (telemetry,
 * track position) of one driver in one time window, or a whole-session list (positions,
 * race control, weather...). Only finished sessions are stored, as their data no longer
 * changes; it is read back before any request, also offline, until the 30-day cleanup
 * ([CacheMaintenance]). Live sessions always go to the network. Without a database (Web)
 * every call goes to the network.
 */
internal class SamplesCache(private val db: FastPilotDB?, private val sessions: SessionRegistry) {
    private val json = Json { ignoreUnknownKeys = true }

    /** A whole-session list (no driver, no time window). */
    suspend fun <T> getOrFetchSession(
        endpoint: String,
        sessionKey: Int,
        serializer: KSerializer<List<T>>,
        fetch: suspend () -> List<T>
    ): List<T> = getOrFetch(endpoint, sessionKey, ALL_DRIVERS, "", "", serializer, fetch)

    suspend fun <T> getOrFetch(
        endpoint: String,
        sessionKey: Int,
        driverNumber: Int,
        from: String,
        to: String,
        serializer: KSerializer<List<T>>,
        fetch: suspend () -> List<T>
    ): List<T> {
        val queries = db?.fastPilotDBQueries ?: return fetch()
        queries.selectSamples(endpoint, sessionKey.toLong(), driverNumber.toLong(), from, to)
            .executeAsOneOrNull()
            ?.let { return json.decodeFromString(serializer, it) }

        val samples = fetch()
        val now = Clock.System.now().toEpochMilliseconds()
        if (samples.isNotEmpty() && sessions.isFinal(sessionKey, now)) {
            queries.insertSamples(
                endpoint, sessionKey.toLong(), driverNumber.toLong(), from, to,
                json.encodeToString(serializer, samples), now
            )
        }
        return samples
    }
}
