package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.DriverRepository
import com.fastpilot.data.repository.PositionRepository
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetLivePositionsUseCaseTest {

    private fun useCase(api: OpenF1Api) = GetLivePositionsUseCase(PositionRepository(api), DriverRepository(api, db = null))

    @Test
    fun `a finished session's positions come once with the drivers in order`() = runTest {
        val api = OpenF1Api(mockHttpClient { request ->
            when {
                request.url.encodedPath.endsWith("/position") -> respondJson("""[
                    {"driver_number":16,"position":2,"date":"2026-10-11T13:50:00+00:00"},
                    {"driver_number":1,"position":1,"date":"2026-10-11T13:50:00+00:00"}]""")
                request.url.encodedPath.endsWith("/drivers") -> respondJson("""[
                    {"driver_number":1,"name_acronym":"VER"},{"driver_number":16,"name_acronym":"LEC"}]""")
                else -> error("unexpected ${request.url}")
            }
        })

        val positions = useCase(api).once(sessionKey = 11388)

        assertEquals(listOf("VER", "LEC"), positions.map { it.driver?.nameAcronym })
    }

    @Test
    fun `without network and without cache the failure reaches the screen`() = runTest {
        // Offline with a session never opened: before, the Dashboard waited forever on "Carregando dados"
        val api = OpenF1Api(mockHttpClient { error("Unable to resolve host api.openf1.org") })

        assertFailsWith<Exception> { useCase(api).once(sessionKey = 11384) }
    }
}
