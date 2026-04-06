package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.PositionRepository
import com.fastpilot.model.Driver
import com.fastpilot.model.Position
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

data class PositionWithDriver(
    val position: Position,
    val driver: Driver?
)

class GetLivePositionsUseCase(
    private val positionRepo: PositionRepository,
    private val driverRepo: DriverRepository
) {
    fun invoke(sessionKey: Int, pollIntervalMs: Long = 4_000L): Flow<List<PositionWithDriver>> = flow {
        // Cache drivers once per session
        val drivers = try {
            driverRepo.getDriversBySession(sessionKey)
        } catch (_: Exception) { emptyList() }

        val driverMap = drivers.associateBy { it.driverNumber }

        while (currentCoroutineContext().isActive) {
            try {
                val positions = positionRepo.getLatestPositions(sessionKey)
                val enriched = positions.map { pos ->
                    PositionWithDriver(
                        position = pos,
                        driver = driverMap[pos.driverNumber]
                    )
                }.sortedBy { it.position.position }
                emit(enriched)
            } catch (_: Exception) { /* retry */ }
            delay(pollIntervalMs)
        }
    }
}
