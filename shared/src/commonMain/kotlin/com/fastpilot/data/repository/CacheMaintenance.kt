package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/**
 * Keeps the local cache from growing without limit (TCC I §4.5): race data — drivers, laps,
 * stints and raw samples — is deleted 30 days after it was first downloaded. Data of a
 * finished session is never downloaded again, so its download time is when it was first
 * opened. The session calendar and the standings are small and stay.
 */
class CacheMaintenance(private val db: FastPilotDB?) {

    companion object {
        val RETENTION = 30.days
    }

    fun purgeExpired(now: Instant = Clock.System.now()) {
        val queries = db?.fastPilotDBQueries ?: return
        val cutoff = (now - RETENTION).toEpochMilliseconds()
        queries.transaction {
            queries.clearDriverCache(cutoff)
            queries.clearLapCache(cutoff)
            queries.clearStintCache(cutoff)
            queries.clearSamplesCache(cutoff)
        }
    }
}
