package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Stint

/**
 * Calculates tyre degradation per stint.
 * Measures how lap time increases relative to tyre age using linear regression.
 */
class GetTyreDegradationUseCase {

    data class DegradationCurve(
        val driverNumber: Int,
        val stintNumber: Int,
        val compound: String,
        val dataPoints: List<DegradationPoint>,
        val degradationPerLap: Double, // seconds per lap (slope)
        val intercept: Double // base lap time at tyre age 0
    )

    data class DegradationPoint(
        val lapNumber: Int,
        val tyreAge: Int,
        val lapTime: Double
    )

    fun invoke(
        laps: List<Lap>,
        stints: List<Stint>,
        driverNumber: Int,
        raceControlMessages: List<RaceControlMessage> = emptyList()
    ): List<DegradationCurve> {
        val driverStints = stints.filter { it.driverNumber == driverNumber }
        // Excludes pit in/out laps and safety car / VSC / red flag laps (TCC I §4.7)
        val driverLaps = LapFilters.representativeLaps(laps, driverNumber, raceControlMessages)
            .associateBy { it.lapNumber }

        return driverStints.mapNotNull { stint ->
            val lapRange = (stint.lapStart ?: return@mapNotNull null)..(stint.lapEnd ?: return@mapNotNull null)
            val tyreAgeStart = stint.tyreAgeAtStart ?: 0

            val points = lapRange.mapNotNull { lapNum ->
                val lap = driverLaps[lapNum] ?: return@mapNotNull null
                DegradationPoint(
                    lapNumber = lapNum,
                    tyreAge = tyreAgeStart + (lapNum - lapRange.first),
                    lapTime = lap.lapDuration!!
                )
            }

            if (points.size < 3) return@mapNotNull null // need at least 3 points

            // Simple linear regression: lapTime = slope * tyreAge + intercept
            val (slope, intercept) = linearRegression(
                x = points.map { it.tyreAge.toDouble() },
                y = points.map { it.lapTime }
            )

            DegradationCurve(
                driverNumber = driverNumber,
                stintNumber = stint.stintNumber,
                compound = stint.compound ?: "UNKNOWN",
                dataPoints = points,
                degradationPerLap = slope,
                intercept = intercept
            )
        }
    }

    private fun linearRegression(x: List<Double>, y: List<Double>): Pair<Double, Double> {
        val n = x.size
        if (n < 2) return Pair(0.0, y.firstOrNull() ?: 0.0)

        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumX2 = x.sumOf { it * it }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        return Pair(slope, intercept)
    }
}
