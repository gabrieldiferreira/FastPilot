package com.fastpilot.network

import com.fastpilot.testing.respondJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JolpicaApiProxyTest {

    private val standings = """
        {"MRData":{"StandingsTable":{"StandingsLists":[{"season":"2026","round":"17",
        "DriverStandings":[{"position":"1","points":"340","wins":"8","Driver":{"driverId":"antonelli","code":"ANT"}}]}]}}}
    """.trimIndent()

    private val requested = mutableListOf<String>()

    /** Same plugins as the app's client (retries, timeouts), on a mock engine. */
    private fun api(proxy: suspend MockRequestHandleScope.() -> HttpResponseData) = JolpicaApi(
        client = HttpClient(MockEngine { request: HttpRequestData ->
            requested += request.url.host + request.url.encodedPath
            if (request.url.host == "proxy.test") proxy() else respondJson(standings)
        }) { installFastPilotPlugins() },
        proxyUrl = "https://proxy.test/api/jolpica"
    )

    @Test
    fun `standings come through the proxy which asks Jolpica for the given path`() = runTest {
        var path: String? = null
        val api = JolpicaApi(
            client = HttpClient(MockEngine { request ->
                requested += request.url.host + request.url.encodedPath
                path = request.url.parameters["path"]
                respondJson(standings)
            }) { installFastPilotPlugins() },
            proxyUrl = "https://proxy.test/api/jolpica"
        )
        val response = api.getDriverStandings(2026)

        assertEquals(listOf("proxy.test/api/jolpica"), requested)
        assertEquals("2026/driverStandings.json", path)
        assertEquals("340", response.mrData?.standingsTable?.standingsLists?.single()?.driverStandings?.single()?.points)
    }

    @Test
    fun `a failing proxy falls back to Jolpica at once without retrying the proxy`() = runTest {
        val response = api { respondJson("""{"detail":"bad gateway"}""", HttpStatusCode.BadGateway) }
            .getDriverStandings(2026)

        assertEquals(listOf("proxy.test/api/jolpica", "api.jolpi.ca/ergast/f1/2026/driverStandings.json"), requested)
        assertEquals("ANT", response.mrData?.standingsTable?.standingsLists?.single()?.driverStandings?.single()?.driver?.code)
    }

    @Test
    fun `an unreachable proxy falls back to Jolpica`() = runTest {
        api { throw IllegalStateException("no route to proxy") }.getConstructorStandings(2026)

        assertEquals("api.jolpi.ca/ergast/f1/2026/constructorStandings.json", requested.last())
    }
}
