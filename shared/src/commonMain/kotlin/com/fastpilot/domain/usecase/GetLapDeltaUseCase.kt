package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.LocationRepository
import com.fastpilot.model.Lap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches the on-track positions of two laps and computes where the compared
 * driver gains or loses time against the reference driver ([CalculateLapDeltaUseCase]).
 */
class GetLapDeltaUseCase(
    private val locationRepo: LocationRepository,
    private val calculateLapDelta: CalculateLapDeltaUseCase
) {
    suspend operator fun invoke(sessionKey: Int, referenceLap: Lap, comparedLap: Lap): LapDelta? = coroutineScope {
        val referenceLocations = async { locationRepo.getLapLocations(sessionKey, referenceLap) }
        val comparedLocations = async { locationRepo.getLapLocations(sessionKey, comparedLap) }
        calculateLapDelta(referenceLap, referenceLocations.await(), comparedLap, comparedLocations.await())
    }
}
