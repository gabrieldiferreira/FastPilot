package com.fastpilot.network

import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.model.Session
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

/**
 * Remembers the sessions the app has listed, so that requests can tell what state a
 * session key is in: live data goes through the OpenF1 proxy and skips the local cache,
 * while data downloaded after a session ended is final and is never downloaded again.
 * Filled by SessionRepository whenever it returns sessions.
 */
class SessionRegistry {
    companion object {
        /** After this long past the scheduled end, OpenF1 no longer changes a session's data. */
        val FINAL_AFTER_END = 1.hours
    }

    private var sessions: Map<Int, Session> = emptyMap()

    fun track(list: List<Session>) {
        sessions = sessions + list.associateBy { it.sessionKey }
    }

    fun isLive(sessionKey: Int?, now: Instant = Clock.System.now()): Boolean =
        sessionKey != null && sessions[sessionKey]?.let { LiveSessionDetector.isLive(it, now) } == true

    /**
     * True when data of [sessionKey] downloaded at [downloadedAt] (epoch ms) is final:
     * it was downloaded once the session was over, so it can be kept for good.
     */
    fun isFinal(sessionKey: Int, downloadedAt: Long): Boolean {
        val end = sessions[sessionKey]?.dateEnd
            ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return downloadedAt >= (end + FINAL_AFTER_END).toEpochMilliseconds()
    }
}
