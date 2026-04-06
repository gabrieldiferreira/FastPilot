package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.TelemetryRepository
import com.fastpilot.model.CarData
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class GetLiveTelemetryUseCase(
    private val telemetryRepo: TelemetryRepository
) {
    /**
     * Observe car telemetry data with polling.
     * If [dateGte] is provided, only fetches data after that timestamp (incremental).
     */
    fun invoke(
        sessionKey: Int,
        driverNumber: Int,
        pollIntervalMs: Long = 1_000L
    ): Flow<List<CarData>> = flow {
        var lastDate: String? = null
        while (currentCoroutineContext().isActive) {
            try {
                val data = telemetryRepo.getCarData(
                    sessionKey = sessionKey,
                    driverNumber = driverNumber,
                    dateGte = lastDate
                )
                if (data.isNotEmpty()) {
                    lastDate = data.last().date
                    emit(data)
                }
            } catch (_: Exception) { /* retry */ }
            delay(pollIntervalMs)
        }
    }

    /**
     * Fetch telemetry for a specific time range (historical).
     */
    suspend fun getForTimeRange(
        sessionKey: Int,
        driverNumber: Int,
        dateGte: String,
        dateLte: String
    ): List<CarData> = telemetryRepo.getCarData(
        sessionKey = sessionKey,
        driverNumber = driverNumber,
        dateGte = dateGte,
        dateLte = dateLte
    )
}
