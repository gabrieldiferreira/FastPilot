package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.OvertakeRepository
import com.fastpilot.data.repository.PitStopRepository
import com.fastpilot.model.Overtake
import com.fastpilot.model.PitStop
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetRaceEventsUseCaseTest {

    @Test
    fun `pit stops and overtakes are merged newest first`() {
        val events = GetRaceEventsUseCase.merge(
            pitStops = listOf(
                PitStop(driverNumber = 44, lapNumber = 20, stopDuration = 2.4, date = "2024-12-08T13:40:00+00:00")
            ),
            overtakes = listOf(
                Overtake(overtakingDriverNumber = 1, overtakenDriverNumber = 55, position = 3, date = "2024-12-08T13:03:42+00:00"),
                Overtake(overtakingDriverNumber = 63, overtakenDriverNumber = 10, position = 5, date = "2024-12-08T13:50:00+00:00")
            )
        )

        assertEquals(listOf(63, 44, 1), events.map { it.driverNumber })
        assertEquals(RaceEvent.Type.PIT_STOP, events[1].type)
        assertEquals(2.4, events[1].stopDuration)
        assertEquals(10, events[0].otherDriverNumber)
        assertEquals(5, events[0].position)
    }

    @Test
    fun `the feed keeps the latest events only`() {
        val overtakes = (0 until 50).map {
            Overtake(overtakingDriverNumber = it, overtakenDriverNumber = 99, date = "2024-12-08T13:${it.toString().padStart(2, '0')}:00+00:00")
        }
        val events = GetRaceEventsUseCase.merge(emptyList(), overtakes)

        assertEquals(GetRaceEventsUseCase.MAX_EVENTS, events.size)
        assertEquals(49, events.first().driverNumber)
    }

    @Test
    fun `an empty pit list answered with 404 and a failing source still give a feed`() = runTest {
        val api = OpenF1Api(mockHttpClient { request ->
            when {
                // OpenF1 answers 404 instead of an empty list
                request.url.encodedPath.endsWith("/pit") ->
                    respondJson("""{"detail":"No results found."}""", HttpStatusCode.NotFound)
                request.url.encodedPath.endsWith("/overtakes") ->
                    respondJson("""[{"overtaking_driver_number":1,"overtaken_driver_number":55,"position":3,"date":"2024-12-08T13:03:42+00:00"}]""")
                else -> error("unexpected request")
            }
        })
        val events = GetRaceEventsUseCase(PitStopRepository(api), OvertakeRepository(api))(sessionKey = 9662)
        assertEquals(1, events.size)

        val failing = OpenF1Api(mockHttpClient { error("offline") })
        assertEquals(emptyList(), GetRaceEventsUseCase(PitStopRepository(failing), OvertakeRepository(failing))(9662))
    }
}
