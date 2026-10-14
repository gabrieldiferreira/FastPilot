package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.PositionRepository
import com.fastpilot.model.Driver
import com.fastpilot.model.Position
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        // Drivers (once per session) and the first positions are fetched together: the screen
        // waits for both, and the requests are queued by the OpenF1 rate limit
        val (drivers, firstPositions) = coroutineScope {
            val drivers = async {
                try { driverRepo.getDriversBySession(sessionKey) } catch (_: Exception) { emptyList() }
            }
            val positions = async {
                try { positionRepo.getLatestPositions(sessionKey) } catch (_: Exception) { null }
            }
            drivers.await() to positions.await()
        }

        val driverMap = drivers.associateBy { it.driverNumber }
        var pending = firstPositions

        while (currentCoroutineContext().isActive) {
            try {
                val positions = pending ?: positionRepo.getLatestPositions(sessionKey)
                pending = null
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
