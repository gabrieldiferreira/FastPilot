package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.Lap
import com.fastpilot.network.OpenF1Api
import kotlinx.datetime.Clock

/**
 * Cache-first repository. [db] is null on platforms without a local database (Web),
 * in which case every call goes straight to the network.
 */
class LapRepository(
    private val api: OpenF1Api,
    private val db: FastPilotDB?
) {
    companion object {
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour
    }

    suspend fun getLaps(
        sessionKey: Int,
        driverNumber: Int,
        forceRefresh: Boolean = false
    ): List<Lap> {
        // A live session keeps changing: always fetch it
        if (!forceRefresh && !api.sessionRegistry.isLive(sessionKey)) {
            val cached = db?.fastPilotDBQueries?.selectLapsByDriverAndSession(
                driverNumber.toLong(), sessionKey.toLong()
            )?.executeAsList().orEmpty()
            if (cached.isNotEmpty()) {
                val cachedAt = cached.first().cached_at
                val age = Clock.System.now().toEpochMilliseconds() - cachedAt
                // Data downloaded after the session ended never changes: kept until the 30-day cleanup
                if (age < CACHE_TTL_MS || api.sessionRegistry.isFinal(sessionKey, cachedAt)) {
                    return cached.map { it.toLap() }
                }
            }
        }

        return try {
            val remote = api.getLaps(sessionKey = sessionKey, driverNumber = driverNumber)
            val now = Clock.System.now().toEpochMilliseconds()
            remote.forEach { lap ->
                db?.fastPilotDBQueries?.insertLap(
                    driver_number = lap.driverNumber.toLong(),
                    session_key = (lap.sessionKey ?: sessionKey).toLong(),
                    lap_number = lap.lapNumber.toLong(),
                    lap_duration = lap.lapDuration,
                    sector_1 = lap.sector1,
                    sector_2 = lap.sector2,
                    sector_3 = lap.sector3,
                    i1_speed = lap.i1Speed?.toLong(),
                    i2_speed = lap.i2Speed?.toLong(),
                    st_speed = lap.stSpeed?.toLong(),
                    is_pit_out_lap = if (lap.isPitOutLap) 1L else 0L,
                    date_start = lap.dateStart,
                    cached_at = now
                )
            }
            remote
        } catch (e: Exception) {
            val cached = db?.fastPilotDBQueries?.selectLapsByDriverAndSession(
                driverNumber.toLong(), sessionKey.toLong()
            )?.executeAsList().orEmpty()
            if (cached.isNotEmpty()) cached.map { it.toLap() }
            else throw e
        }
    }

    suspend fun getAllLaps(sessionKey: Int): List<Lap> {
        return try {
            api.getLaps(sessionKey = sessionKey)
        } catch (_: Exception) { emptyList() }
    }
}

private fun com.fastpilot.db.Cached_lap.toLap() = Lap(
    driverNumber = driver_number.toInt(),
    lapNumber = lap_number.toInt(),
    lapDuration = lap_duration,
    dateStart = date_start,
    sector1 = sector_1,
    sector2 = sector_2,
    sector3 = sector_3,
    i1Speed = i1_speed?.toInt(),
    i2Speed = i2_speed?.toInt(),
    stSpeed = st_speed?.toInt(),
    isPitOutLap = is_pit_out_lap == 1L,
    sessionKey = session_key.toInt()
)
