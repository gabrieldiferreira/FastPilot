package com.fastpilot.data.repository

import com.fastpilot.network.JolpicaApi
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StandingsRepositoryTest {

    // Trimmed real Jolpica responses (2025 season, after round 24)
    private val driverStandingsJson = """
        {"MRData":{"total":"2","StandingsTable":{"season":"2025","round":"24","StandingsLists":[{"season":"2025","round":"24",
        "DriverStandings":[
          {"position":"1","positionText":"1","points":"423","wins":"7",
           "Driver":{"driverId":"norris","code":"NOR","givenName":"Lando","familyName":"Norris"},
           "Constructors":[{"constructorId":"mclaren","name":"McLaren"}]},
          {"position":"2","positionText":"2","points":"421.5","wins":"8",
           "Driver":{"driverId":"max_verstappen","givenName":"Max","familyName":"Verstappen"},
           "Constructors":[{"constructorId":"red_bull","name":"Red Bull"}]}
        ]}]}}}
    """.trimIndent()

    private val constructorStandingsJson = """
        {"MRData":{"total":"1","StandingsTable":{"season":"2025","round":"24","StandingsLists":[{"season":"2025","round":"24",
        "ConstructorStandings":[
          {"position":"1","positionText":"1","points":"833","wins":"14",
           "Constructor":{"constructorId":"mclaren","name":"McLaren"}}
        ]}]}}}
    """.trimIndent()

    private fun repository() = StandingsRepository(
        JolpicaApi(mockHttpClient { request ->
            if ("constructorStandings" in request.url.encodedPath) respondJson(constructorStandingsJson)
            else respondJson(driverStandingsJson)
        }),
        db = null
    )

    @Test
    fun `maps Jolpica driver and constructor standings`() = runTest {
        val standings = repository().getStandings(2025)

        assertEquals(2025, standings.season)
        assertEquals(24, standings.round)

        val norris = standings.drivers[0]
        assertEquals(1, norris.position)
        assertEquals("NOR", norris.code)
        assertEquals("Lando Norris", norris.fullName)
        assertEquals("mclaren", norris.constructorId)
        assertEquals(423.0, norris.points)
        assertEquals(7, norris.wins)

        val mclaren = standings.constructors.single()
        assertEquals("McLaren", mclaren.name)
        assertEquals(833.0, mclaren.points)
        assertEquals(14, mclaren.wins)
    }

    @Test
    fun `parses fractional points and derives missing driver code`() = runTest {
        val verstappen = repository().getStandings(2025).drivers[1]

        assertEquals(421.5, verstappen.points)
        assertEquals("VER", verstappen.code)
    }

    @Test
    fun `without cache a network error is propagated`() = runTest {
        val repo = StandingsRepository(
            JolpicaApi(mockHttpClient { throw IllegalStateException("offline") }),
            db = null
        )

        assertFailsWith<Exception> { repo.getStandings(2025) }
    }
}
