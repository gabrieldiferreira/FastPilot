package com.fastpilot.network

import com.fastpilot.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Client for the OpenF1 API (https://api.openf1.org/v1).
 * Provides access to all 18 endpoints covering F1 telemetry, timing, and session data.
 * Historical data from 2023 onwards is free; real-time data requires paid subscription.
 */
class OpenF1Api(private val client: HttpClient) {

    companion object {
        private const val BASE_URL = "https://api.openf1.org/v1"
    }

    // ── Sessions & Meetings ─────────────────────────────────────────────

    suspend fun getSessions(
        year: Int? = null,
        countryName: String? = null,
        sessionName: String? = null,
        sessionKey: Int? = null,
        meetingKey: Int? = null
    ): List<Session> = client.get("$BASE_URL/sessions") {
        year?.let { parameter("year", it) }
        countryName?.let { parameter("country_name", it) }
        sessionName?.let { parameter("session_name", it) }
        sessionKey?.let { parameter("session_key", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }.body()

    suspend fun getMeetings(
        year: Int? = null,
        countryName: String? = null,
        meetingKey: Int? = null
    ): List<Meeting> = client.get("$BASE_URL/meetings") {
        year?.let { parameter("year", it) }
        countryName?.let { parameter("country_name", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }.body()

    // ── Drivers ──────────────────────────────────────────────────────────

    suspend fun getDrivers(
        sessionKey: Int? = null,
        driverNumber: Int? = null,
        meetingKey: Int? = null
    ): List<Driver> = client.get("$BASE_URL/drivers") {
        sessionKey?.let { parameter("session_key", it) }
        driverNumber?.let { parameter("driver_number", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }.body()

    // ── Telemetry (Car Data) ────────────────────────────────────────────

    suspend fun getCarData(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String? = null,
        dateLte: String? = null,
        speedGte: Int? = null
    ): List<CarData> = client.get("$BASE_URL/car_data") {
        parameter("session_key", sessionKey)
        parameter("driver_number", driverNumber)
        dateGte?.let { parameter("date>", it) }
        dateLte?.let { parameter("date<", it) }
        speedGte?.let { parameter("speed>=", it) }
    }.body()

    // ── Laps ─────────────────────────────────────────────────────────────

    suspend fun getLaps(
        sessionKey: Int,
        driverNumber: Int? = null,
        lapNumber: Int? = null
    ): List<Lap> = client.get("$BASE_URL/laps") {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
        lapNumber?.let { parameter("lap_number", it) }
    }.body()

    // ── Stints ───────────────────────────────────────────────────────────

    suspend fun getStints(
        sessionKey: Int,
        driverNumber: Int? = null,
        stintNumber: Int? = null
    ): List<Stint> = client.get("$BASE_URL/stints") {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
        stintNumber?.let { parameter("stint_number", it) }
    }.body()

    // ── Positions ────────────────────────────────────────────────────────

    suspend fun getPositions(
        sessionKey: Int? = null,
        meetingKey: Int? = null,
        driverNumber: Int? = null
    ): List<Position> = client.get("$BASE_URL/position") {
        sessionKey?.let { parameter("session_key", it) }
        meetingKey?.let { parameter("meeting_key", it) }
        driverNumber?.let { parameter("driver_number", it) }
    }.body()

    // ── Intervals ────────────────────────────────────────────────────────

    suspend fun getIntervals(
        sessionKey: Int,
        driverNumber: Int? = null
    ): List<Interval> = client.get("$BASE_URL/intervals") {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
    }.body()

    // ── Race Control ─────────────────────────────────────────────────────

    suspend fun getRaceControl(
        sessionKey: Int? = null,
        flag: String? = null,
        driverNumber: Int? = null,
        dateGte: String? = null,
        dateLte: String? = null
    ): List<RaceControlMessage> = client.get("$BASE_URL/race_control") {
        sessionKey?.let { parameter("session_key", it) }
        flag?.let { parameter("flag", it) }
        driverNumber?.let { parameter("driver_number", it) }
        dateGte?.let { parameter("date>=", it) }
        dateLte?.let { parameter("date<", it) }
    }.body()

    // ── Weather ──────────────────────────────────────────────────────────

    suspend fun getWeather(
        sessionKey: Int? = null,
        meetingKey: Int? = null
    ): List<Weather> = client.get("$BASE_URL/weather") {
        sessionKey?.let { parameter("session_key", it) }
        meetingKey?.let { parameter("meeting_key", it) }
    }.body()

    // ── Team Radio ───────────────────────────────────────────────────────

    suspend fun getTeamRadio(
        sessionKey: Int,
        driverNumber: Int? = null
    ): List<TeamRadio> = client.get("$BASE_URL/team_radio") {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
    }.body()

    // ── Pit Stops ────────────────────────────────────────────────────────

    suspend fun getPitStops(
        sessionKey: Int,
        driverNumber: Int? = null
    ): List<PitStop> = client.get("$BASE_URL/pit") {
        parameter("session_key", sessionKey)
        driverNumber?.let { parameter("driver_number", it) }
    }.body()

    // ── Location ─────────────────────────────────────────────────────────

    suspend fun getLocation(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String? = null,
        dateLte: String? = null
    ): List<Location> = client.get("$BASE_URL/location") {
        parameter("session_key", sessionKey)
        parameter("driver_number", driverNumber)
        dateGte?.let { parameter("date>", it) }
        dateLte?.let { parameter("date<", it) }
    }.body()
}
