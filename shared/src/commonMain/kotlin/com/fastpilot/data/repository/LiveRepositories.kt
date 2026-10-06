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
import com.fastpilot.network.OpenF1Api
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Repository for telemetry data (car_data endpoint).
 * No local cache — telemetry is always fetched fresh due to high volume.
 */
class TelemetryRepository(private val api: OpenF1Api) {

    suspend fun getCarData(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String? = null,
        dateLte: String? = null
    ): List<CarData> = api.getCarData(sessionKey, driverNumber, dateGte, dateLte)
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
 * No local cache — only the samples of the requested lap are fetched (~350 per lap).
 */
class LocationRepository(private val api: OpenF1Api) {

    suspend fun getLapLocations(sessionKey: Int, lap: Lap): List<Location> {
        val start = lap.dateStart?.let(Instant::parse) ?: return emptyList()
        val duration = lap.lapDuration ?: return emptyList()
        val end = start + (duration * 1000).toLong().milliseconds + 1.seconds
        return api.getLocation(sessionKey, lap.driverNumber, dateGte = start.toString(), dateLte = end.toString())
    }
}
