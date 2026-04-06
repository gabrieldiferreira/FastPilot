package com.fastpilot.domain.usecase

import com.fastpilot.model.Session
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Detects whether a given session is currently live by comparing
 * its dateStart/dateEnd against the current system clock.
 *
 * A session is considered "live" if: dateStart <= now <= dateEnd
 */
object LiveSessionDetector {

    fun isLive(session: Session): Boolean {
        val now = Clock.System.now()
        val start = session.dateStart?.let { parseInstant(it) } ?: return false
        val end = session.dateEnd?.let { parseInstant(it) } ?: return false
        return now in start..end
    }

    /**
     * From a list of sessions, finds the one that is currently live (if any).
     */
    fun findLiveSession(sessions: List<Session>): Session? =
        sessions.firstOrNull { isLive(it) }

    private fun parseInstant(dateString: String): Instant? = try {
        Instant.parse(dateString)
    } catch (_: Exception) {
        null
    }
}
