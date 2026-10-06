package com.fastpilot.fixtures

import com.fastpilot.data.repository.LapRepository
import com.fastpilot.domain.usecase.CalculateCleanPaceUseCase
import com.fastpilot.domain.usecase.CalculateLapDeltaUseCase
import com.fastpilot.domain.usecase.CalculateTheoreticalBestUseCase
import com.fastpilot.domain.usecase.GetDriverDuelUseCase
import com.fastpilot.domain.usecase.GetTyreDegradationUseCase
import com.fastpilot.model.Driver
import com.fastpilot.model.Interval
import com.fastpilot.model.Lap
import com.fastpilot.model.Location
import com.fastpilot.model.PitStop
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Session
import com.fastpilot.model.Stint
import com.fastpilot.model.Weather
import com.fastpilot.network.JolpicaResponse
import com.fastpilot.network.OpenF1Api
import com.fastpilot.network.RaceTable
import io.ktor.client.HttpClient
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Checks that the recorded API responses in fixtures/ (scripts/fetch_fixtures.py)
 * decode with the app's models and that the four metrics produce sane values on
 * real race data. These fixtures feed the FastF1 comparison harness (Fase 4).
 */
class RaceFixturesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Gradle runs tests from the module directory
    private val root = File("../fixtures")

    private val races = listOf("2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil")

    private inline fun <reified T> openF1(race: String, endpoint: String): List<T> =
        json.decodeFromString(File(root, "$race/openf1/$endpoint.json").readText())

    private fun winnerNumber(race: String): Int {
        val results = json.decodeFromString<JolpicaResponse<RaceTable>>(
            File(root, "$race/jolpica/results.json").readText()
        )
        return results.mrData!!.raceTable!!.races.single().results.first { it.position == "1" }.number!!.toInt()
    }

    @Test
    fun `all fixtures decode with the app models`() {
        for (race in races) {
            val session = openF1<Session>(race, "sessions").single()
            assertEquals("Race", session.sessionName, race)
            assertTrue(openF1<Driver>(race, "drivers").size >= 19, "$race drivers") // OpenF1 omits Stroll (withdrew) in Singapore 2023
            assertTrue(openF1<Lap>(race, "laps").size > 700, "$race laps")
            assertTrue(openF1<Stint>(race, "stints").isNotEmpty(), "$race stints")
            assertTrue(openF1<RaceControlMessage>(race, "race_control").isNotEmpty(), "$race race_control")
            assertTrue(openF1<Weather>(race, "weather").isNotEmpty(), "$race weather")
            assertTrue(openF1<Interval>(race, "intervals").isNotEmpty(), "$race intervals")
            openF1<PitStop>(race, "pit") // may legitimately be empty (no pit data for Bahrain 2023)
        }
    }

    @Test
    fun `jolpica lap timings are complete`() {
        for (race in races) {
            val laps = json.decodeFromString<JsonObject>(
                File(root, "$race/jolpica/laps.json").readText()
            )
            val mrData = laps["MRData"]!!.toString()
            val total = Regex("\"total\":\"(\\d+)\"").find(mrData)!!.groupValues[1].toInt()
            val timings = Regex("\"position\":").findAll(mrData).count()
            assertEquals(total, timings, "$race: merged pages must contain every lap timing")
        }
    }

    @Test
    fun `metrics produce sane values for each race winner`() {
        val cleanPace = CalculateCleanPaceUseCase()
        val theoreticalBest = CalculateTheoreticalBestUseCase()
        val degradation = GetTyreDegradationUseCase()

        for (race in races) {
            val laps = openF1<Lap>(race, "laps")
            val stints = openF1<Stint>(race, "stints")
            val raceControl = openF1<RaceControlMessage>(race, "race_control")
            val winner = winnerNumber(race)

            val pace = cleanPace.invoke(laps, winner, raceControl)
            assertTrue(pace.cleanLapCount > 10, "$race clean laps: ${pace.cleanLapCount}")
            assertTrue(pace.averagePace in 60.0..150.0, "$race average pace: ${pace.averagePace}")

            val best = theoreticalBest.invoke(laps, winner)!!
            assertTrue(best.theoreticalBestTime in 60.0..150.0, "$race theoretical best: ${best.theoreticalBestTime}")
            assertTrue(best.gap >= -1e-6, "$race: theoretical best can't beat the actual best lap (gap ${best.gap})")

            assertTrue(degradation.invoke(laps, stints, winner).isNotEmpty(), "$race degradation curves")
        }
    }

    @Test
    fun `duel between winner and runner-up covers most of the race`() {
        val duel = GetDriverDuelUseCase(LapRepository(OpenF1Api(HttpClient()), db = null))
        for (race in races) {
            val laps = openF1<Lap>(race, "laps")
            val results = json.decodeFromString<JolpicaResponse<RaceTable>>(
                File(root, "$race/jolpica/results.json").readText()
            ).mrData!!.raceTable!!.races.single().results
            val (p1, p2) = results.take(2).map { it.number!!.toInt() }

            val result = duel.calculate(p1, p2, laps.filter { it.driverNumber == p1 }, laps.filter { it.driverNumber == p2 })

            assertTrue(result.cumulativeDelta.size > 20, "$race common laps: ${result.cumulativeDelta.size}")
        }
    }

    /** Official circuit lengths (metres) for the fixture races. */
    private val circuitLength = mapOf(
        "2023_bahrain" to 5412.0, "2023_belgium" to 7004.0, "2023_singapore" to 4928.0,
        "2024_monaco" to 3337.0, "2024_brazil" to 4309.0
    )

    @Test
    fun `lap delta between the two fastest finishers is consistent with official lap times`() {
        val calculate = CalculateLapDeltaUseCase()
        for (race in races) {
            val laps = openF1<Lap>(race, "laps")
            val locationFiles = File(root, "$race/openf1").listFiles { f -> f.name.startsWith("location_") }!!
            fun fastestLapAndLocations(driver: Int): Pair<Lap, List<Location>> {
                val file = locationFiles.single { it.name.startsWith("location_${driver}_") }
                val lapNumber = file.name.substringAfter("_lap").removeSuffix(".json").toInt()
                return laps.single { it.driverNumber == driver && it.lapNumber == lapNumber } to
                    json.decodeFromString<List<Location>>(file.readText())
            }
            val results = json.decodeFromString<JolpicaResponse<RaceTable>>(
                File(root, "$race/jolpica/results.json").readText()
            ).mrData!!.raceTable!!.races.single().results
            val (p1, p2) = results.take(2).map { it.number!!.toInt() }
            val (lap1, loc1) = fastestLapAndLocations(p1)
            val (lap2, loc2) = fastestLapAndLocations(p2)

            val delta = calculate(lap1, loc1, lap2, loc2)!!

            val lengthError = (delta.lapLength - circuitLength.getValue(race)) / circuitLength.getValue(race)
            val lastComputed = delta.points[delta.points.size - 2]
            val closingError = lastComputed.delta - delta.finalGap
            println(
                "$race #$p1 L${lap1.lapNumber} vs #$p2 L${lap2.lapNumber}: traced %.0f m (%+.1f%%), ".format(delta.lapLength, lengthError * 100) +
                    "official gap %+.3f s, curve at %.0f m %+.3f s (closing error %+.3f s), range %+.2f..%+.2f s, %d points".format(
                        delta.finalGap, lastComputed.distance, lastComputed.delta, closingError,
                        delta.points.minOf { it.delta }, delta.points.maxOf { it.delta }, delta.points.size
                    )
            )
            assertTrue(kotlin.math.abs(lengthError) < 0.05, "$race traced length ${delta.lapLength}")
            assertTrue(delta.points.size > 200, "$race points ${delta.points.size}")
            assertTrue(kotlin.math.abs(closingError) < 0.3, "$race closing error $closingError")
        }
    }
}
