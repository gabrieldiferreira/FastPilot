package com.fastpilot.evaluation

import com.fastpilot.domain.usecase.CalculateCleanPaceUseCase
import com.fastpilot.domain.usecase.CalculateLapDeltaUseCase
import com.fastpilot.domain.usecase.CalculateTheoreticalBestUseCase
import com.fastpilot.domain.usecase.GetTyreDegradationUseCase
import com.fastpilot.model.Lap
import com.fastpilot.model.Location
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Stint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import kotlin.math.roundToLong

/**
 * Runs FastPilot's metric use cases on the recorded fixtures (TCC II, Fase 4) and writes
 * metrics/comparison/fastpilot/<race>.json in the same shape as scripts/fastf1_reference.py.
 *
 * Run from the repository root: ./gradlew :shared:exportMetrics [--args=<output dir>]
 */
private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val prettyJson = Json { prettyPrint = true }

private val races = listOf("2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil")

private fun round(value: Double, decimals: Int = 4): Double {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    return (value * factor).roundToLong() / factor
}

private inline fun <reified T> File.decode(): T = json.decodeFromString(readText())

fun main(args: Array<String>) {
    val root = File("fixtures")
    require(root.isDirectory) { "Run from the repository root (fixtures/ not found in ${File(".").absolutePath})" }
    val out = File(args.firstOrNull() ?: "metrics/comparison/fastpilot").apply { mkdirs() }

    val cleanPace = CalculateCleanPaceUseCase()
    val degradation = GetTyreDegradationUseCase()
    val theoreticalBest = CalculateTheoreticalBestUseCase()
    val lapDelta = CalculateLapDeltaUseCase()

    for (race in races) {
        val dir = File(root, "$race/openf1")
        val laps = File(dir, "laps.json").decode<List<Lap>>()
        val stints = File(dir, "stints.json").decode<List<Stint>>()
        val raceControl = File(dir, "race_control.json").decode<List<RaceControlMessage>>()

        val drivers = buildJsonObject {
            for (driver in laps.map { it.driverNumber }.distinct().sorted()) {
                putJsonObject(driver.toString()) {
                    val pace = cleanPace.invoke(laps, driver, raceControl)
                    val best = theoreticalBest.invoke(laps, driver)
                    if (pace.cleanLapCount > 0) {
                        put("clean_pace_mean", round(pace.averagePace))
                        put("clean_pace_median", round(pace.medianPace))
                    }
                    put("clean_lap_count", pace.cleanLapCount)
                    best?.let {
                        put("theoretical_best", round(it.theoreticalBestTime))
                        put("best_lap", round(it.actualBestTime))
                    }
                    putJsonArray("stints") {
                        for (curve in degradation.invoke(laps, stints, driver, raceControl)) {
                            val stint = stints.first { it.driverNumber == driver && it.stintNumber == curve.stintNumber }
                            add(buildJsonObject {
                                put("stint", curve.stintNumber)
                                put("compound", curve.compound)
                                put("lap_start", stint.lapStart)
                                put("lap_end", stint.lapEnd)
                                put("laps", curve.dataPoints.size)
                                put("slope", round(curve.degradationPerLap, 5))
                                put("intercept", round(curve.intercept))
                            })
                        }
                    }
                }
            }
        }

        // Fastest laps with recorded positions: location_<driver>_lap<N>.json, in finishing order of the fixtures
        val locationFiles = dir.listFiles { f -> f.name.startsWith("location_") }!!.associateBy {
            it.name.removePrefix("location_").substringBefore("_").toInt()
        }
        val top3 = File(root, "$race/jolpica/results.json").readText().let { text ->
            Regex("\"number\":\\s*\"(\\d+)\"").findAll(text).map { it.groupValues[1].toInt() }.take(3).toList()
        }
        fun fastest(driver: Int): Pair<Lap, List<Location>> {
            val file = locationFiles.getValue(driver)
            val lapNumber = file.name.substringAfter("_lap").removeSuffix(".json").toInt()
            return laps.single { it.driverNumber == driver && it.lapNumber == lapNumber } to file.decode()
        }

        val deltas = buildJsonArray {
            for ((ref, cmp) in listOf(top3[0] to top3[1], top3[0] to top3[2], top3[1] to top3[2])) {
                val (refLap, refLoc) = fastest(ref)
                val (cmpLap, cmpLoc) = fastest(cmp)
                val delta = lapDelta(refLap, refLoc, cmpLap, cmpLoc) ?: continue
                add(buildJsonObject {
                    put("reference", ref)
                    put("compared", cmp)
                    put("reference_lap", refLap.lapNumber)
                    put("compared_lap", cmpLap.lapNumber)
                    put("lap_length", round(delta.lapLength, 1))
                    put("fraction", JsonArray(delta.points.map { JsonPrimitive(round(it.distance / delta.lapLength, 5)) }))
                    put("delta", JsonArray(delta.points.map { JsonPrimitive(round(it.delta)) }))
                    put("official_gap", round(delta.finalGap))
                })
            }
        }

        val report = JsonObject(mapOf("race" to JsonPrimitive(race), "drivers" to drivers, "lap_deltas" to deltas))
        File(out, "$race.json").writeText(prettyJson.encodeToString(JsonObject.serializer(), report) + "\n")
        println("$race: ${drivers.size} drivers, ${deltas.size} lap deltas")
    }
}
