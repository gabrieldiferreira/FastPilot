package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.Session
import com.fastpilot.network.OpenF1Api
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Sessions loaded in this run, by year, with when they were fetched: the only cache on Web. */
    private val memory = mutableMapOf<Int, Pair<List<Session>, Long>>()
    private val seasonsMutex = Mutex()
    // Screens load in the background, on several threads: guards [memory] and the session registry
    private val memoryMutex = Mutex()

    /** Forgets the sessions kept in memory (the benchmark's loads without cache). */
    suspend fun clearMemory() = memoryMutex.withLock { memory.clear() }

    /** Sessions of [year], newest first. Also tells the API which of them can be live. */
    suspend fun getSessions(year: Int, forceRefresh: Boolean = false): List<Session> =
        loadSessions(year, forceRefresh).also { memoryMutex.withLock { api.sessionRegistry.track(it) } }

    /**
     * Sessions of every year in [years], newest first by year. The years not cached yet come
     * in a single request ("year>=") instead of one per year; a year that fails is left out.
     * Concurrent callers wait for each other, so screens opening together share the requests.
     */
    suspend fun getSessionsOfYears(years: List<Int>): List<Session> = seasonsMutex.withLock {
        val byYear = mutableMapOf<Int, List<Session>>()
        years.forEach { year -> cachedSessions(year)?.let { byYear[year] = it } }
        val missing = years - byYear.keys
        if (missing.size > 1) {
            try {
                val remote = api.getSessions(fromYear = missing.min())
                // Every OpenF1 session has its year; without it (or with no sessions at all)
                // the list cannot be split, so each year is asked for on its own
                if (remote.isNotEmpty() && remote.all { it.year != null }) {
                    val remoteByYear = remote.groupBy { it.year }
                    missing.forEach { year -> byYear[year] = store(year, remoteByYear[year].orEmpty()) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Falls back to one request per year below, which also falls back to the cache
            }
        }
        (years - byYear.keys).forEach { year ->
            try {
                byYear[year] = loadSessions(year, forceRefresh = false)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        years.flatMap { byYear[it].orEmpty() }.also { memoryMutex.withLock { api.sessionRegistry.track(it) } }
    }

    /** The cached sessions of [year] while still valid: memory first, then the database. */
    private suspend fun cachedSessions(year: Int): List<Session>? {
        val now = Clock.System.now().toEpochMilliseconds()
        // The calendar of a past season no longer changes
        fun valid(cachedAt: Long) = now - cachedAt < CACHE_TTL_MS || year < availableSeasons().first()
        memoryMutex.withLock { memory[year] }?.let { (sessions, cachedAt) -> if (valid(cachedAt)) return sessions }
        val cached = db?.fastPilotDBQueries?.selectSessionsByYear(year.toLong())?.executeAsList().orEmpty()
        // Rows cached before v4 have no time zone: fetched again once
        if (cached.isNotEmpty() && valid(cached.first().cached_at) && cached.none { it.gmt_offset.isEmpty() }) {
            return cached.map { it.toSession() }.also { memoryMutex.withLock { memory[year] = it to cached.first().cached_at } }
        }
        return null
    }

    /** Keeps [sessions] of [year] in memory and in the database, and returns them newest first. */
    private suspend fun store(year: Int, sessions: List<Session>): List<Session> {
        // Newest first, the same order as selectSessionsByYear: screens pick the first race,
        // which must not change depending on whether the list came from the cache
        val sorted = sessions.sortedByDescending { it.dateStart.orEmpty() }
        val now = Clock.System.now().toEpochMilliseconds()
        memoryMutex.withLock { memory[year] = sorted to now }
        sorted.forEach { session ->
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
                cached_at = now,
                gmt_offset = session.gmtOffset ?: ""
            )
        }
        return sorted
    }

    private suspend fun loadSessions(year: Int, forceRefresh: Boolean): List<Session> {
        if (!forceRefresh) cachedSessions(year)?.let { return it }

        return try {
            store(year, api.getSessions(year = year))
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
    year = year.toInt(),
    gmtOffset = gmt_offset.ifEmpty { null }
)
