package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.Driver
import com.fastpilot.network.OpenF1Api
import kotlinx.datetime.Clock

class DriverRepository(
    private val api: OpenF1Api,
    private val db: FastPilotDB
) {
    companion object {
        private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours
    }

    suspend fun getDriversBySession(sessionKey: Int, forceRefresh: Boolean = false): List<Driver> {
        if (!forceRefresh) {
            val cached = db.fastPilotDBQueries.selectDriversBySession(sessionKey.toLong()).executeAsList()
            if (cached.isNotEmpty()) {
                val age = Clock.System.now().toEpochMilliseconds() - cached.first().cached_at
                if (age < CACHE_TTL_MS) {
                    return cached.map { it.toDriver() }
                }
            }
        }

        return try {
            val remote = api.getDrivers(sessionKey = sessionKey)
            val now = Clock.System.now().toEpochMilliseconds()
            remote.forEach { driver ->
                db.fastPilotDBQueries.insertDriver(
                    driver_number = driver.driverNumber.toLong(),
                    session_key = (driver.sessionKey ?: sessionKey).toLong(),
                    full_name = driver.fullName ?: "${driver.firstName ?: ""} ${driver.lastName ?: ""}".trim(),
                    name_acronym = driver.nameAcronym ?: "",
                    team_name = driver.teamName ?: "",
                    team_colour = driver.teamColour ?: "FFFFFF",
                    headshot_url = driver.headshotUrl,
                    broadcast_name = driver.broadcastName,
                    cached_at = now
                )
            }
            remote
        } catch (e: Exception) {
            val cached = db.fastPilotDBQueries.selectDriversBySession(sessionKey.toLong()).executeAsList()
            if (cached.isNotEmpty()) cached.map { it.toDriver() }
            else throw e
        }
    }

    suspend fun getDriverByNumber(sessionKey: Int, driverNumber: Int): Driver? {
        val cached = db.fastPilotDBQueries.selectDriverByNumber(
            driverNumber.toLong(), sessionKey.toLong()
        ).executeAsOneOrNull()
        if (cached != null) return cached.toDriver()

        return try {
            api.getDrivers(sessionKey = sessionKey, driverNumber = driverNumber).firstOrNull()
        } catch (_: Exception) { null }
    }
}

private fun com.fastpilot.db.Cached_driver.toDriver() = Driver(
    driverNumber = driver_number.toInt(),
    fullName = full_name,
    nameAcronym = name_acronym,
    teamName = team_name,
    teamColour = team_colour,
    headshotUrl = headshot_url,
    broadcastName = broadcast_name,
    sessionKey = session_key.toInt()
)
