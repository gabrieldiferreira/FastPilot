package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client for the Jolpica API (Ergast-compatible).
 * Provides historical F1 data like race results, standings, and season info.
 * Base URL: https://api.jolpi.ca/ergast/f1/
 */
class JolpicaApi(private val client: HttpClient) {

    companion object {
        private const val BASE_URL = "https://api.jolpi.ca/ergast/f1"
    }

    suspend fun getSeasons(): JolpicaResponse<SeasonTable> =
        client.get("$BASE_URL/seasons.json?limit=100").body()

    suspend fun getRaces(year: Int): JolpicaResponse<RaceTable> =
        client.get("$BASE_URL/$year/races.json").body()

    suspend fun getRaceResults(year: Int, round: Int): JolpicaResponse<RaceTable> =
        client.get("$BASE_URL/$year/$round/results.json").body()

    suspend fun getDriverStandings(year: Int): JolpicaResponse<StandingsTable> =
        client.get("$BASE_URL/$year/driverStandings.json").body()

    suspend fun getConstructorStandings(year: Int): JolpicaResponse<StandingsTable> =
        client.get("$BASE_URL/$year/constructorStandings.json").body()
}

// ── Response wrapper models ─────────────────────────────────────────────

@Serializable
data class JolpicaResponse<T>(
    @SerialName("MRData") val mrData: MRData<T>? = null
)

@Serializable
data class MRData<T>(
    val xmlns: String? = null,
    val series: String? = null,
    val url: String? = null,
    val limit: String? = null,
    val offset: String? = null,
    val total: String? = null,
    @SerialName("SeasonTable") val seasonTable: SeasonTable? = null,
    @SerialName("RaceTable") val raceTable: RaceTable? = null,
    @SerialName("StandingsTable") val standingsTable: StandingsTable? = null
)

@Serializable
data class SeasonTable(
    @SerialName("Seasons") val seasons: List<JolpicaSeason> = emptyList()
)

@Serializable
data class JolpicaSeason(
    val season: String? = null,
    val url: String? = null
)

@Serializable
data class RaceTable(
    val season: String? = null,
    @SerialName("Races") val races: List<JolpicaRace> = emptyList()
)

@Serializable
data class JolpicaRace(
    val season: String? = null,
    val round: String? = null,
    val url: String? = null,
    val raceName: String? = null,
    @SerialName("Circuit") val circuit: JolpicaCircuit? = null,
    val date: String? = null,
    val time: String? = null,
    @SerialName("Results") val results: List<JolpicaResult> = emptyList()
)

@Serializable
data class JolpicaCircuit(
    val circuitId: String? = null,
    val url: String? = null,
    val circuitName: String? = null,
    @SerialName("Location") val location: JolpicaLocation? = null
)

@Serializable
data class JolpicaLocation(
    val lat: String? = null,
    @SerialName("long") val lng: String? = null,
    val locality: String? = null,
    val country: String? = null
)

@Serializable
data class JolpicaResult(
    val number: String? = null,
    val position: String? = null,
    val positionText: String? = null,
    val points: String? = null,
    @SerialName("Driver") val driver: JolpicaDriver? = null,
    @SerialName("Constructor") val constructor: JolpicaConstructor? = null,
    val grid: String? = null,
    val laps: String? = null,
    val status: String? = null,
    @SerialName("Time") val time: JolpicaTime? = null
)

@Serializable
data class JolpicaDriver(
    val driverId: String? = null,
    val permanentNumber: String? = null,
    val code: String? = null,
    val url: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val dateOfBirth: String? = null,
    val nationality: String? = null
)

@Serializable
data class JolpicaConstructor(
    val constructorId: String? = null,
    val url: String? = null,
    val name: String? = null,
    val nationality: String? = null
)

@Serializable
data class JolpicaTime(
    val millis: String? = null,
    val time: String? = null
)

@Serializable
data class StandingsTable(
    val season: String? = null,
    @SerialName("StandingsLists") val standingsLists: List<StandingsList> = emptyList()
)

@Serializable
data class StandingsList(
    val season: String? = null,
    val round: String? = null,
    @SerialName("DriverStandings") val driverStandings: List<DriverStanding> = emptyList(),
    @SerialName("ConstructorStandings") val constructorStandings: List<ConstructorStanding> = emptyList()
)

@Serializable
data class DriverStanding(
    val position: String? = null,
    val positionText: String? = null,
    val points: String? = null,
    val wins: String? = null,
    @SerialName("Driver") val driver: JolpicaDriver? = null,
    @SerialName("Constructors") val constructors: List<JolpicaConstructor> = emptyList()
)

@Serializable
data class ConstructorStanding(
    val position: String? = null,
    val positionText: String? = null,
    val points: String? = null,
    val wins: String? = null,
    @SerialName("Constructor") val constructor: JolpicaConstructor? = null
)
