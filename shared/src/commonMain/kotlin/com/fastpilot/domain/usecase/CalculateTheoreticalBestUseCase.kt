package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap

/**
 * Calculates the theoretical best lap time by taking the minimum sector times.
 * TheoreticalBest = min(S1) + min(S2) + min(S3)
 */
class CalculateTheoreticalBestUseCase {

    data class TheoreticalBest(
        val driverNumber: Int,
        val bestSector1: Double,
        val bestSector1Lap: Int,
        val bestSector2: Double,
        val bestSector2Lap: Int,
        val bestSector3: Double,
        val bestSector3Lap: Int,
        val theoreticalBestTime: Double,
        val actualBestTime: Double,
        val gap: Double // difference between actual best and theoretical
    )

    fun invoke(laps: List<Lap>, driverNumber: Int): TheoreticalBest? {
        val driverLaps = laps.filter {
            it.driverNumber == driverNumber &&
            !it.isPitOutLap &&
            it.sector1 != null && it.sector2 != null && it.sector3 != null
        }

        if (driverLaps.isEmpty()) return null

        val bestS1Lap = driverLaps.minByOrNull { it.sector1!! }!!
        val bestS2Lap = driverLaps.minByOrNull { it.sector2!! }!!
        val bestS3Lap = driverLaps.minByOrNull { it.sector3!! }!!

        val bestS1 = bestS1Lap.sector1!!
        val bestS2 = bestS2Lap.sector2!!
        val bestS3 = bestS3Lap.sector3!!
        val theoreticalTime = bestS1 + bestS2 + bestS3

        val actualBest = driverLaps.filter { it.lapDuration != null }
            .minByOrNull { it.lapDuration!! }?.lapDuration ?: theoreticalTime

        return TheoreticalBest(
            driverNumber = driverNumber,
            bestSector1 = bestS1,
            bestSector1Lap = bestS1Lap.lapNumber,
            bestSector2 = bestS2,
            bestSector2Lap = bestS2Lap.lapNumber,
            bestSector3 = bestS3,
            bestSector3Lap = bestS3Lap.lapNumber,
            theoreticalBestTime = theoreticalTime,
            actualBestTime = actualBest,
            gap = actualBest - theoreticalTime
        )
    }
}
