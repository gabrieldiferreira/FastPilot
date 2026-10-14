package com.fastpilot.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fastpilot.db.FastPilotDB
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Screens open the first race of the list, so the network and the cache must return
 * sessions in the same order; otherwise the default race changes after the first visit.
 */
class SessionRepositoryOrderTest {

    // OpenF1 lists sessions oldest first
    private val sessionsJson = """
        [
          {"session_key":9693,"session_name":"Race","session_type":"Race","country_name":"Australia","date_start":"2025-03-16T04:00:00+00:00","date_end":"2025-03-16T06:00:00+00:00","year":2025},
          {"session_key":9998,"session_name":"Race","session_type":"Race","country_name":"China","date_start":"2025-03-23T07:00:00+00:00","date_end":"2025-03-23T09:00:00+00:00","year":2025},
          {"session_key":9839,"session_name":"Race","session_type":"Race","country_name":"United Arab Emirates","date_start":"2025-12-07T13:00:00+00:00","date_end":"2025-12-07T15:00:00+00:00","year":2025}
        ]
    """.trimIndent()

    @Test
    fun `network and cache return sessions newest first`() = runTest {
        val db = FastPilotDB(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FastPilotDB.Schema.create(it) })
        val repository = SessionRepository(OpenF1Api(mockHttpClient { respondJson(sessionsJson) }), db)

        val fromNetwork = repository.getSessions(2025).map { it.sessionKey }
        val fromCache = repository.getSessions(2025).map { it.sessionKey }

        assertEquals(listOf(9839, 9998, 9693), fromNetwork)
        assertEquals(fromNetwork, fromCache)
    }
}
