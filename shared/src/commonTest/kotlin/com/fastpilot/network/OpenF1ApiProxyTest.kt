package com.fastpilot.network

import com.fastpilot.model.Session
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class OpenF1ApiProxyTest {

    private val proxy = "https://fastpilot.example/openf1/v1"
    private val now = Clock.System.now()

    private val liveSession = Session(sessionKey = 1, dateStart = "${now - 1.hours}", dateEnd = "${now + 1.hours}")
    private val pastSession = Session(sessionKey = 2, dateStart = "${now - 5.hours}", dateEnd = "${now - 3.hours}")

    private fun api(proxyBaseUrl: String, unauthorizedHost: String? = null): Pair<OpenF1Api, List<String>> {
        val requested = mutableListOf<String>()
        val api = OpenF1Api(mockHttpClient { request ->
            requested += request.url.toString()
            if (request.url.host == unauthorizedHost) respondJson("""{"detail":"Unauthorized"}""", HttpStatusCode.Unauthorized)
            else respondJson("[]")
        }, proxyBaseUrl)
        api.sessionRegistry.track(listOf(liveSession, pastSession))
        return api to requested
    }

    @Test
    fun `live sessions go through the proxy`() = runTest {
        val (api, requested) = api(proxy)
        api.getPositions(sessionKey = 1)
        assertEquals(listOf("$proxy/position?session_key=1"), requested)
    }

    @Test
    fun `past sessions go straight to OpenF1`() = runTest {
        val (api, requested) = api(proxy)
        api.getPositions(sessionKey = 2)
        assertEquals(listOf("https://api.openf1.org/v1/position?session_key=2"), requested)
    }

    @Test
    fun `a request refused with 401 is repeated through the proxy`() = runTest {
        val (api, requested) = api(proxy, unauthorizedHost = "api.openf1.org")
        api.getSessions(year = 2026)
        assertEquals(
            listOf("https://api.openf1.org/v1/sessions?year=2026", "$proxy/sessions?year=2026"),
            requested
        )
    }

    @Test
    fun `without a proxy live sessions are requested directly`() = runTest {
        val (api, requested) = api("")
        api.getPositions(sessionKey = 1)
        assertEquals(listOf("https://api.openf1.org/v1/position?session_key=1"), requested)
    }
}
