package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.retry
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client for the Jolpica API (Ergast-compatible).
 * Provides historical F1 data like race results, standings, and season info.
 * Base URL: https://api.jolpi.ca/ergast/f1/
 *
 * Requests go first through [proxyUrl] (Vercel function api/jolpica.mjs), whose CDN keeps
 * the answers: Jolpica itself takes from 0.5 s to over 20 s when its own cache has expired.
 * When the proxy fails or is slow, the request goes straight to Jolpica. Blank = no proxy.
 */
class JolpicaApi(
    private val client: HttpClient,
    private val proxyUrl: String = JOLPICA_PROXY_URL
) {

    companion object {
        private const val BASE_URL = "https://api.jolpi.ca/ergast/f1"
        private const val PROXY_TIMEOUT_MS = 10_000L
    }

    private suspend inline fun <reified T> get(path: String): T {
        if (proxyUrl.isNotBlank()) {
            try {
                val response = client.get(proxyUrl) {
                    parameter("path", path)
                    // A failing proxy falls back at once instead of waiting for the retries
                    retry { noRetry() }
                    timeout { requestTimeoutMillis = PROXY_TIMEOUT_MS }
                }
                if (response.status.isSuccess()) return response.body()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        return client.get("$BASE_URL/$path").body()
    }

    suspend fun getSeasons(): JolpicaResponse<SeasonTable> = get("seasons.json?limit=100")

    suspend fun getRaces(year: Int): JolpicaResponse<RaceTable> = get("$year/races.json")

    suspend fun getRaceResults(year: Int, round: Int): JolpicaResponse<RaceTable> =
        get("$year/$round/results.json")

    suspend fun getDriverStandings(year: Int): JolpicaResponse<StandingsTable> =
        get("$year/driverStandings.json")

    suspend fun getConstructorStandings(year: Int): JolpicaResponse<StandingsTable> =
        get("$year/constructorStandings.json")
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
