package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
}
