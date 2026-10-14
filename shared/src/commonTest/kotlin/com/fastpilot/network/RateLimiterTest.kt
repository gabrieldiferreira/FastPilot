package com.fastpilot.network

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RateLimiterTest {

    @Test
    fun `the fourth request in the window waits for the window to pass`() = runTest {
        val limiter = RateLimiter(permits = 3, windowMs = 1_100)
        val startedAt = (1..5).map { limiter.acquire(); currentTime }
        assertEquals(listOf(0L, 0L, 0L, 1_100L, 2_200L), startedAt)
    }
}
