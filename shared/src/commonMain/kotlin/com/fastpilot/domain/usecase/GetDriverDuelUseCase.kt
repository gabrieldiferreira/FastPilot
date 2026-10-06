package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.LapRepository
import com.fastpilot.model.Lap

data class DuelData(
    val driver1Number: Int,
    val driver2Number: Int,
    val lapsDriver1: List<Lap>,
    val lapsDriver2: List<Lap>,
    val cumulativeDelta: List<DeltaPoint>,
    val sectorComparison: List<SectorComparison>
)

data class DeltaPoint(
    val lapNumber: Int,
    val delta: Double // positive = driver1 ahead, negative = driver2 ahead
)

data class SectorComparison(
    val lapNumber: Int,
    val s1Driver1: Double?,
    val s1Driver2: Double?,
    val s2Driver1: Double?,
    val s2Driver2: Double?,
    val s3Driver1: Double?,
    val s3Driver2: Double?
)

class GetDriverDuelUseCase(
    private val lapRepo: LapRepository
) {
    suspend fun invoke(
        sessionKey: Int,
        driver1Number: Int,
        driver2Number: Int
    ): DuelData {
        val laps1 = lapRepo.getLaps(sessionKey, driver1Number)
        val laps2 = lapRepo.getLaps(sessionKey, driver2Number)
        return calculate(driver1Number, driver2Number, laps1, laps2)
    }

    /**
     * Pure duel calculation over already-fetched laps. Pit-out laps and laps without
     * a duration are ignored; only laps both drivers completed are compared.
     */
    fun calculate(
        driver1Number: Int,
        driver2Number: Int,
        allLapsDriver1: List<Lap>,
        allLapsDriver2: List<Lap>
    ): DuelData {
        val laps1 = allLapsDriver1.filter { !it.isPitOutLap && it.lapDuration != null }
        val laps2 = allLapsDriver2.filter { !it.isPitOutLap && it.lapDuration != null }

        val laps1Map = laps1.associateBy { it.lapNumber }
        val laps2Map = laps2.associateBy { it.lapNumber }

        // Find common laps
        val commonLaps = laps1Map.keys.intersect(laps2Map.keys).sorted()

        // Calculate cumulative delta
        var cumDelta = 0.0
        val deltaPoints = commonLaps.map { lapNum ->
            val time1 = laps1Map[lapNum]!!.lapDuration!!
            val time2 = laps2Map[lapNum]!!.lapDuration!!
            cumDelta += (time2 - time1) // positive = driver1 faster
            DeltaPoint(lapNum, cumDelta)
        }

        // Sector comparison
        val sectorComps = commonLaps.map { lapNum ->
            val l1 = laps1Map[lapNum]!!
            val l2 = laps2Map[lapNum]!!
            SectorComparison(
                lapNumber = lapNum,
                s1Driver1 = l1.sector1, s1Driver2 = l2.sector1,
                s2Driver1 = l1.sector2, s2Driver2 = l2.sector2,
                s3Driver1 = l1.sector3, s3Driver2 = l2.sector3
            )
        }

        return DuelData(
            driver1Number = driver1Number,
            driver2Number = driver2Number,
            lapsDriver1 = laps1,
            lapsDriver2 = laps2,
            cumulativeDelta = deltaPoints,
            sectorComparison = sectorComps
        )
    }
}
