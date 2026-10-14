package com.fastpilot.data.repository

import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.model.Session
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class RecentSessionsTest {

    // Sunday of the 2026 São Paulo GP, during the race
    private val now = Instant.parse("2026-11-08T17:30:00Z")

    private fun session(key: Int, type: String, start: String, end: String) = Session(
        sessionKey = key, sessionName = type, sessionType = type, dateStart = start, dateEnd = end
    )

    private val qualifying = session(1, "Qualifying", "2026-11-07T18:00:00+00:00", "2026-11-07T19:00:00+00:00")
    private val race = session(2, "Race", "2026-11-08T17:00:00+00:00", "2026-11-08T19:00:00+00:00")
    private val nextRace = session(3, "Race", "2026-11-22T04:00:00+00:00", "2026-11-22T06:00:00+00:00")
    private val lastSeasonRace = session(4, "Race", "2025-12-07T13:00:00+00:00", "2025-12-07T15:00:00+00:00")

    @Test
    fun `seasons go from the current year down to 2023`() {
        assertEquals(listOf(2026, 2025, 2024, 2023), availableSeasons(now))
        assertEquals(2027, availableSeasons(Instant.parse("2027-01-01T00:00:00Z")).first())
    }

    @Test
    fun `sessions that have not started are left out`() {
        val started = listOf(nextRace, race, qualifying, lastSeasonRace).startedBy(now)
        assertEquals(listOf(2, 1, 4), started.map { it.sessionKey })
    }

    @Test
    fun `the live session is the default`() {
        val sessions = listOf(race, qualifying, lastSeasonRace)
        assertEquals(2, sessions.defaultSession(now)?.sessionKey)
    }

    @Test
    fun `without a live session the most recent race is the default`() {
        val saturday = Instant.parse("2026-11-07T20:00:00Z")
        val sessions = listOf(race, qualifying, lastSeasonRace).startedBy(saturday)
        // Qualifying is newer but the screens open with a race
        assertEquals(4, sessions.defaultSession(saturday)?.sessionKey)
    }

    @Test
    fun `a session stays live for 30 minutes after its scheduled end`() {
        val end = Instant.parse("2026-11-08T19:00:00Z")
        assertTrue(LiveSessionDetector.isLive(race, end + 29.minutes))
        assertFalse(LiveSessionDetector.isLive(race, end + 31.minutes))
        assertFalse(LiveSessionDetector.isLive(race, Instant.parse("2026-11-08T16:59:00Z")))
    }

    @Test
    fun `season sessions merge every year newest first`() = runTest {
        val repository = SessionRepository(OpenF1Api(mockHttpClient { request ->
            when (request.url.parameters["year"]) {
                "2026" -> respondJson("""[
                    {"session_key":1,"session_type":"Qualifying","date_start":"2026-11-07T18:00:00+00:00"},
                    {"session_key":3,"session_type":"Race","date_start":"2026-11-22T04:00:00+00:00"},
                    {"session_key":2,"session_type":"Race","date_start":"2026-11-08T17:00:00+00:00"}]""")
                "2025" -> respondJson("""[{"session_key":4,"session_type":"Race","date_start":"2025-12-07T13:00:00+00:00"}]""")
                else -> respondJson("[]")
            }
        }), db = null)

        assertEquals(listOf(3, 2, 1, 4), repository.getSeasonSessions(now).map { it.sessionKey })
        assertEquals(listOf(2, 1, 4), repository.getRecentSessions(now).map { it.sessionKey })
    }
}
