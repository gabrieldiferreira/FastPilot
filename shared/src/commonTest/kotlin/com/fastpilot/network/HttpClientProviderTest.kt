package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpClientProviderTest {

    private fun client(statuses: List<HttpStatusCode>): HttpClient {
        var call = 0
        return HttpClient(MockEngine { respond("ok", statuses[minOf(call++, statuses.lastIndex)]) }) {
            installFastPilotPlugins()
        }
    }

    @Test
    fun `rate limited requests are retried and every attempt is counted`() = runTest {
        val requestsBefore = RequestStats.requests
        val limitedBefore = RequestStats.rateLimited
        val client = client(listOf(HttpStatusCode.TooManyRequests, HttpStatusCode.TooManyRequests, HttpStatusCode.OK))

        val body = client.get("https://api.openf1.org/v1/sessions").bodyAsText()

        assertEquals("ok", body)
        assertEquals(3, RequestStats.requests - requestsBefore)
        assertEquals(2, RequestStats.rateLimited - limitedBefore)
    }

    @Test
    fun `client errors other than 429 are not retried`() = runTest {
        val requestsBefore = RequestStats.requests
        val client = client(listOf(HttpStatusCode.NotFound))

        val status = client.get("https://api.openf1.org/v1/pit").status

        assertEquals(HttpStatusCode.NotFound, status)
        assertEquals(1, RequestStats.requests - requestsBefore)
    }

    @Test
    fun `waiting for the OpenF1 quota does not count against the request timeout`() = runTest {
        // Many screens opened in a row: 60 requests at once, twice the 29-per-61-s quota, so the
        // last ones wait in the rate limiter for more than the 60 s request timeout
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { respond("ok") }
        })
        val client = HttpClient(engine) { installFastPilotPlugins() }

        val statuses = (1..60).map { async { client.get("https://api.openf1.org/v1/team_radio").status } }.awaitAll()

        assertEquals(List(60) { HttpStatusCode.OK }, statuses)
    }

    @Test
    fun `without network a request fails after one quick retry`() = runTest {
        // Offline: no answer at all. Before, 5 retries waiting 2+4+8+16+16 s kept the screen loading
        var attempts = 0
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { attempts++; throw kotlinx.io.IOException("Unable to resolve host \"api.openf1.org\"") }
        })
        val client = HttpClient(engine) { installFastPilotPlugins() }

        val failure = runCatching { client.get("https://api.openf1.org/v1/position") }.exceptionOrNull()

        assertEquals(true, failure is kotlinx.io.IOException)
        assertEquals(2, attempts)
        assertEquals(true, currentTime < 5_000, "failed after ${currentTime} ms")
    }
}
