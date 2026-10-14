package com.fastpilot.domain.usecase

import com.fastpilot.model.Session
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Detects whether a given session is currently live by comparing
 * its dateStart/dateEnd against the current system clock.
 *
 * A session is considered "live" if: dateStart <= now <= dateEnd + [GRACE_AFTER_END].
 * The grace period matches OpenF1's real-time window (until 30 min after the session),
 * which covers overruns (dateEnd is the scheduled end) and the final classification.
 */
object LiveSessionDetector {

    val GRACE_AFTER_END = 30.minutes

    fun isLive(session: Session, now: Instant = Clock.System.now()): Boolean {
        val start = session.dateStart?.let { parseInstant(it) } ?: return false
        val end = session.dateEnd?.let { parseInstant(it) } ?: return false
        return now in start..(end + GRACE_AFTER_END)
    }

    /** True once the session has started; sessions without a start date count as started. */
    fun hasStarted(session: Session, now: Instant = Clock.System.now()): Boolean {
        val start = session.dateStart?.let { parseInstant(it) } ?: return true
        return start <= now
    }

    /**
     * From a list of sessions, finds the one that is currently live (if any).
     */
    fun findLiveSession(sessions: List<Session>, now: Instant = Clock.System.now()): Session? =
        sessions.firstOrNull { isLive(it, now) }

    private fun parseInstant(dateString: String): Instant? = try {
        Instant.parse(dateString)
    } catch (_: Exception) {
        null
    }
}
