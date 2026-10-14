package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.Session
import com.fastpilot.network.OpenF1Api
import kotlinx.datetime.Clock

/**
 * Cache-first repository. [db] is null on platforms without a local database (Web),
 * in which case every call goes straight to the network.
 */
class SessionRepository(
    private val api: OpenF1Api,
    private val db: FastPilotDB?
) {
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /** Sessions of [year], newest first. Also tells the API which of them can be live. */
    suspend fun getSessions(year: Int, forceRefresh: Boolean = false): List<Session> =
        loadSessions(year, forceRefresh).also { api.sessionRegistry.track(it) }

    private suspend fun loadSessions(year: Int, forceRefresh: Boolean): List<Session> {
        if (!forceRefresh) {
            val cached = db?.fastPilotDBQueries?.selectSessionsByYear(year.toLong())?.executeAsList().orEmpty()
            if (cached.isNotEmpty()) {
                val age = Clock.System.now().toEpochMilliseconds() - cached.first().cached_at
                // The calendar of a past season no longer changes
                if (age < CACHE_TTL_MS || year < availableSeasons().first()) {
                    return cached.map { it.toSession() }
                }
            }
        }

        return try {
            // Newest first, the same order as selectSessionsByYear: screens pick the first race,
            // which must not change depending on whether the list came from the cache
            val remote = api.getSessions(year = year).sortedByDescending { it.dateStart.orEmpty() }
            val now = Clock.System.now().toEpochMilliseconds()
            remote.forEach { session ->
                db?.fastPilotDBQueries?.insertSession(
                    session_key = session.sessionKey.toLong(),
                    session_name = session.sessionName ?: "",
                    session_type = session.sessionType ?: "",
                    meeting_key = (session.meetingKey ?: 0).toLong(),
                    circuit_short_name = session.circuitShortName ?: "",
                    country_name = session.countryName ?: "",
                    country_code = session.countryCode ?: "",
                    date_start = session.dateStart ?: "",
                    date_end = session.dateEnd ?: "",
                    year = (session.year ?: year).toLong(),
                    cached_at = now
                )
            }
            remote
        } catch (e: Exception) {
            // Fallback to cached data on network error
            val cached = db?.fastPilotDBQueries?.selectSessionsByYear(year.toLong())?.executeAsList().orEmpty()
            if (cached.isNotEmpty()) cached.map { it.toSession() }
            else throw e
        }
    }

    suspend fun getSessionByKey(sessionKey: Int): Session? {
        val cached = db?.fastPilotDBQueries?.selectSessionByKey(sessionKey.toLong())?.executeAsOneOrNull()
        if (cached != null) return cached.toSession()

        return try {
            val remote = api.getSessions(sessionKey = sessionKey)
            remote.firstOrNull()
        } catch (_: Exception) { null }
    }

    suspend fun getSessionsByMeeting(meetingKey: Int): List<Session> {
        val cached = db?.fastPilotDBQueries?.selectSessionsByMeeting(meetingKey.toLong())?.executeAsList().orEmpty()
        if (cached.isNotEmpty()) return cached.map { it.toSession() }

        return try {
            api.getSessions(meetingKey = meetingKey)
        } catch (_: Exception) { emptyList() }
    }
}

// Extension to convert from DB model to domain model
private fun com.fastpilot.db.Cached_session.toSession() = Session(
    sessionKey = session_key.toInt(),
    sessionName = session_name,
    sessionType = session_type,
    meetingKey = meeting_key.toInt(),
    circuitShortName = circuit_short_name,
    countryName = country_name,
    countryCode = country_code,
    dateStart = date_start,
    dateEnd = date_end,
    year = year.toInt()
)
