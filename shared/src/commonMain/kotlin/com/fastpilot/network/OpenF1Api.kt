package com.fastpilot.network

import com.fastpilot.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode

/**
 * Client for the OpenF1 API (https://api.openf1.org/v1).
 * Provides access to all 18 endpoints covering F1 telemetry, timing, and session data.
 * Historical data from 2023 onwards is free; real-time data requires paid subscription,
 * so requests about a live session go through the proxy at [proxyBaseUrl] (see [OPENF1_PROXY_URL]).
 */
class OpenF1Api(
    private val client: HttpClient,
    private val proxyBaseUrl: String = OPENF1_PROXY_URL,
    val sessionRegistry: SessionRegistry = SessionRegistry()
) {

    companion object {
        private const val BASE_URL = "https://api.openf1.org/v1"
    }

    private suspend inline fun <reified T> get(
        endpoint: String,
        sessionKey: Int?,
        crossinline block: HttpRequestBuilder.() -> Unit
    ): T {
        val proxy = proxyBaseUrl.takeIf { it.isNotBlank() }
        if (proxy != null && sessionRegistry.isLive(sessionKey)) {
            return client.get("$proxy/$endpoint") { block() }.body()
        }
        val response = client.get("$BASE_URL/$endpoint") { block() }
        // While a session is live OpenF1 may refuse unauthenticated requests: retry through the proxy
        if (proxy != null && response.status == HttpStatusCode.Unauthorized) {
            return client.get("$proxy/$endpoint") { block() }.body()
        }
        // OpenF1 answers 404 {"detail":"No results found."} instead of an empty list
        // (e.g. /pit of a race without stops); every endpoint returns a list
        @Suppress("UNCHECKED_CAST")
        if (response.status == HttpStatusCode.NotFound) return emptyList<Any>() as T
        return response.body()
    }

    // ── Sessions & Meetings ─────────────────────────────────────────────

    suspend fun getSessions(
        year: Int? = null,
        countryName: String? = null,
        sessionName: String? = null,
        sessionKey: Int? = null,
        meetingKey: Int? = null,
        /** Every year from this one on ("year>=2023"), in one request. */
        fromYear: Int? = null
    ): List<Session> = get("sessions", sessionKey) {
        year?.let { parameter("year", it) }
        fromYear?.let { parameter("year>", it) }
        countryName?.let { parameter("country_name", it) }
        sessionName?.let { parameter("session_name", it) }
        sessionKey?.let { parameter("session_key", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }

    suspend fun getMeetings(
        year: Int? = null,
        countryName: String? = null,
        meetingKey: Int? = null
    ): List<Meeting> = get("meetings", null) {
        year?.let { parameter("year", it) }
        countryName?.let { parameter("country_name", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }

    // ── Drivers ──────────────────────────────────────────────────────────

    suspend fun getDrivers(
        sessionKey: Int? = null,
        driverNumber: Int? = null,
        meetingKey: Int? = null
    ): List<Driver> = get("drivers", sessionKey) {
        sessionKey?.let { parameter("session_key", it) }
        driverNumber?.let { parameter("driver_number", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }

    // ── Telemetry (Car Data) ────────────────────────────────────────────

    suspend fun getCarData(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String? = null,
        dateLte: String? = null,
        speedGte: Int? = null
    ): List<CarData> = get("car_data", sessionKey) {
        parameter("session_key", sessionKey)
        parameter("driver_number", driverNumber)
        dateGte?.let { parameter("date>", it) }
        dateLte?.let { parameter("date<", it) }
        speedGte?.let { parameter("speed>=", it) }
    }

    // ── Laps ─────────────────────────────────────────────────────────────

    suspend fun getLaps(
        sessionKey: Int,
        driverNumber: Int? = null,
        lapNumber: Int? = null
    ): List<Lap> = get("laps", sessionKey) {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
        lapNumber?.let { parameter("lap_number", it) }
    }

    // ── Stints ───────────────────────────────────────────────────────────

    suspend fun getStints(
        sessionKey: Int,
        driverNumber: Int? = null,
        stintNumber: Int? = null
    ): List<Stint> = get("stints", sessionKey) {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
        stintNumber?.let { parameter("stint_number", it) }
    }

    // ── Positions ────────────────────────────────────────────────────────

    suspend fun getPositions(
        sessionKey: Int? = null,
        meetingKey: Int? = null,
        driverNumber: Int? = null
    ): List<Position> = get("position", sessionKey) {
        sessionKey?.let { parameter("session_key", it) }
        meetingKey?.let { parameter("meeting_key", it) }
        driverNumber?.let { parameter("driver_number", it) }
    }

    // ── Intervals ────────────────────────────────────────────────────────

    suspend fun getIntervals(
        sessionKey: Int,
        driverNumber: Int? = null
    ): List<Interval> = get("intervals", sessionKey) {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
    }

    // ── Race Control ─────────────────────────────────────────────────────

    suspend fun getRaceControl(
        sessionKey: Int? = null,
        flag: String? = null,
        driverNumber: Int? = null,
        dateGte: String? = null,
        dateLte: String? = null
    ): List<RaceControlMessage> = get("race_control", sessionKey) {
        sessionKey?.let { parameter("session_key", it) }
        flag?.let { parameter("flag", it) }
        driverNumber?.let { parameter("driver_number", it) }
        dateGte?.let { parameter("date>=", it) }
        dateLte?.let { parameter("date<", it) }
    }

    // ── Weather ──────────────────────────────────────────────────────────

    suspend fun getWeather(
        sessionKey: Int? = null,
        meetingKey: Int? = null
    ): List<Weather> = get("weather", sessionKey) {
        sessionKey?.let { parameter("session_key", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }

    // ── Team Radio ───────────────────────────────────────────────────────

    suspend fun getTeamRadio(
        sessionKey: Int,
        driverNumber: Int? = null
    ): List<TeamRadio> = get("team_radio", sessionKey) {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
    }

    // ── Pit Stops ────────────────────────────────────────────────────────

    suspend fun getPitStops(
        sessionKey: Int,
        driverNumber: Int? = null
    ): List<PitStop> = get("pit", sessionKey) {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
    }

    // ── Overtakes ────────────────────────────────────────────────────────

    suspend fun getOvertakes(sessionKey: Int): List<Overtake> = get("overtakes", sessionKey) {
        parameter("session_key", sessionKey)
    }

    // ── Location ─────────────────────────────────────────────────────────

    suspend fun getLocation(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String? = null,
        dateLte: String? = null
    ): List<Location> = get("location", sessionKey) {
        parameter("session_key", sessionKey)
        parameter("driver_number", driverNumber)
        dateGte?.let { parameter("date>", it) }
        dateLte?.let { parameter("date<", it) }
    }
}
