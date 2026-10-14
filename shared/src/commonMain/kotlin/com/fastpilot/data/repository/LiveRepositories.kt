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
 */
class PositionRepository(private val api: OpenF1Api) {

    suspend fun getPositions(sessionKey: Int, driverNumber: Int? = null): List<Position> =
        api.getPositions(sessionKey = sessionKey, driverNumber = driverNumber)

    suspend fun getLatestPositions(sessionKey: Int): List<Position> {
        val all = api.getPositions(sessionKey = sessionKey)
        // Group by driver, keep last position for each
        return all.groupBy { it.driverNumber }
            .mapValues { (_, positions) -> positions.last() }
            .values
            .sortedBy { it.position }
    }
}

/**
 * Repository for interval/gap data (polling-friendly).
 */
class IntervalRepository(private val api: OpenF1Api) {

    suspend fun getIntervals(sessionKey: Int, driverNumber: Int? = null): List<Interval> =
        api.getIntervals(sessionKey, driverNumber)

    suspend fun getLatestIntervals(sessionKey: Int): List<Interval> {
        val all = api.getIntervals(sessionKey)
        return all.groupBy { it.driverNumber }
            .mapValues { (_, intervals) -> intervals.last() }
            .values
            .toList()
    }
}

/**
 * Repository for race control messages.
 */
class RaceControlRepository(private val api: OpenF1Api) {

    suspend fun getRaceControl(sessionKey: Int): List<RaceControlMessage> =
        api.getRaceControl(sessionKey = sessionKey)

    suspend fun getFlags(sessionKey: Int): List<RaceControlMessage> =
        api.getRaceControl(sessionKey = sessionKey).filter { it.category == "Flag" }
}

/**
 * Repository for weather data.
 */
class WeatherRepository(private val api: OpenF1Api) {

    suspend fun getWeather(sessionKey: Int): List<Weather> =
        api.getWeather(sessionKey = sessionKey)

    suspend fun getLatestWeather(sessionKey: Int): Weather? =
        api.getWeather(sessionKey = sessionKey).lastOrNull()
}

/**
 * Repository for team radio communications.
 */
class TeamRadioRepository(private val api: OpenF1Api) {

    suspend fun getTeamRadio(sessionKey: Int, driverNumber: Int? = null): List<TeamRadio> =
        api.getTeamRadio(sessionKey, driverNumber)
}

/**
 * Repository for pit stop data.
 */
class PitStopRepository(private val api: OpenF1Api) {

    suspend fun getPitStops(sessionKey: Int, driverNumber: Int? = null): List<PitStop> =
        api.getPitStops(sessionKey, driverNumber)
}

/**
 * Repository for overtakes (passes between two cars).
 */
class OvertakeRepository(private val api: OpenF1Api) {

    suspend fun getOvertakes(sessionKey: Int): List<Overtake> = api.getOvertakes(sessionKey)
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

/** End of [lap] plus one second of margin, or null without a start or a lap time. */
private fun lapEnd(lap: Lap): String? {
    val start = lap.dateStart?.let(Instant::parse) ?: return null
    val duration = lap.lapDuration ?: return null
    return (start + (duration * 1000).toLong().milliseconds + 1.seconds).toString()
}

/**
 * Raw samples (telemetry, track position) of one driver in one time window (TCC I §4.5).
 * Only samples of a finished session are stored, as they no longer change; they are read
 * back before any request, also offline, until the 30-day cleanup ([CacheMaintenance]).
 * Without a database (Web) every call goes to the network.
 */
private class SamplesCache(private val db: FastPilotDB?, private val sessions: SessionRegistry) {
    private val json = Json { ignoreUnknownKeys = true }

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
