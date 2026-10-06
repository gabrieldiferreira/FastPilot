package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.RaceControlMessage

/**
 * Calculates clean race pace by removing outlier laps.
 * Filters out: pit in/out laps, safety car / VSC / red flag laps, the top 5% and bottom 5%.
 */
class CalculateCleanPaceUseCase {

    data class CleanPaceStats(
        val driverNumber: Int,
        val averagePace: Double,
        val medianPace: Double,
        val bestLap: Double,
        val consistency: Double, // standard deviation
        val cleanLapCount: Int,
        val totalLapCount: Int,
        val cleanLaps: List<Lap>
    )

    fun invoke(
        laps: List<Lap>,
        driverNumber: Int,
        safetyCarMessages: List<RaceControlMessage> = emptyList()
    ): CleanPaceStats {
        // Pit in/out laps and safety car / VSC / red flag laps are atypical (TCC I §4.7)
        val noScLaps = LapFilters.representativeLaps(laps, driverNumber, safetyCarMessages)

        // Remove top/bottom 5% outliers
        val sorted = noScLaps.sortedBy { it.lapDuration }
        val trimCount = (sorted.size * 0.05).toInt().coerceAtLeast(0)
        val cleanLaps = if (sorted.size > trimCount * 2) {
            sorted.subList(trimCount, sorted.size - trimCount)
        } else {
            sorted
        }

        val durations = cleanLaps.mapNotNull { it.lapDuration }

        val avg = if (durations.isNotEmpty()) durations.average() else 0.0
        val median = if (durations.isNotEmpty()) {
            val mid = durations.size / 2
            if (durations.size % 2 == 0) (durations[mid - 1] + durations[mid]) / 2.0
            else durations[mid]
        } else 0.0
        val best = durations.minOrNull() ?: 0.0
        val stdDev = if (durations.size > 1) {
            val mean = durations.average()
            kotlin.math.sqrt(durations.map { (it - mean) * (it - mean) }.average())
        } else 0.0

        return CleanPaceStats(
            driverNumber = driverNumber,
            averagePace = avg,
            medianPace = median,
            bestLap = best,
            consistency = stdDev,
            cleanLapCount = cleanLaps.size,
            totalLapCount = laps.count { it.driverNumber == driverNumber },
            cleanLaps = cleanLaps
        )
    }
}
