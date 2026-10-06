package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.Stint
import com.fastpilot.network.OpenF1Api
import kotlinx.datetime.Clock

/**
 * Cache-first repository. [db] is null on platforms without a local database (Web),
 * in which case every call goes straight to the network.
 */
class StintRepository(
    private val api: OpenF1Api,
    private val db: FastPilotDB?
) {
    companion object {
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour
    }

    suspend fun getStints(
        sessionKey: Int,
        driverNumber: Int? = null,
        forceRefresh: Boolean = false
    ): List<Stint> {
        if (!forceRefresh && driverNumber != null) {
            val cached = db?.fastPilotDBQueries?.selectStintsByDriverAndSession(
                driverNumber.toLong(), sessionKey.toLong()
            )?.executeAsList().orEmpty()
            if (cached.isNotEmpty()) {
                val age = Clock.System.now().toEpochMilliseconds() - cached.first().cached_at
                if (age < CACHE_TTL_MS) {
                    return cached.map { it.toStint() }
                }
            }
        }

        return try {
            val remote = api.getStints(sessionKey = sessionKey, driverNumber = driverNumber)
            val now = Clock.System.now().toEpochMilliseconds()
            remote.forEach { stint ->
                db?.fastPilotDBQueries?.insertStint(
                    driver_number = stint.driverNumber.toLong(),
                    session_key = (stint.sessionKey ?: sessionKey).toLong(),
                    stint_number = stint.stintNumber.toLong(),
                    compound = stint.compound ?: "UNKNOWN",
                    lap_start = (stint.lapStart ?: 0).toLong(),
                    lap_end = (stint.lapEnd ?: 0).toLong(),
                    tyre_age_at_start = (stint.tyreAgeAtStart ?: 0).toLong(),
                    cached_at = now
                )
            }
            remote
        } catch (e: Exception) {
            if (driverNumber != null) {
                val cached = db?.fastPilotDBQueries?.selectStintsByDriverAndSession(
                    driverNumber.toLong(), sessionKey.toLong()
                )?.executeAsList().orEmpty()
                if (cached.isNotEmpty()) return cached.map { it.toStint() }
            }
            throw e
        }
    }

    suspend fun getAllStints(sessionKey: Int): List<Stint> {
        val cached = db?.fastPilotDBQueries?.selectStintsBySession(sessionKey.toLong())?.executeAsList().orEmpty()
        if (cached.isNotEmpty()) return cached.map { it.toStint() }
        return try { api.getStints(sessionKey = sessionKey) } catch (_: Exception) { emptyList() }
    }
}

private fun com.fastpilot.db.Cached_stint.toStint() = Stint(
    driverNumber = driver_number.toInt(),
    stintNumber = stint_number.toInt(),
    compound = compound,
    lapStart = lap_start.toInt(),
    lapEnd = lap_end.toInt(),
    tyreAgeAtStart = tyre_age_at_start.toInt(),
    sessionKey = session_key.toInt()
)
