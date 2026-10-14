package com.fastpilot.network

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlin.test.Test
import kotlin.test.assertEquals

class RateLimiterTest {

    @Test
    fun `the fourth request in the window waits for the window to pass`() = runTest {
        val limiter = RateLimiter(permits = 3, windowMs = 1_100, timeSource = testTimeSource)
        val startedAt = (1..7).map { limiter.acquire(); currentTime }
        assertEquals(listOf(0L, 0L, 0L, 1_100L, 1_100L, 1_100L, 2_200L), startedAt)
    }

    @Test
    fun `the per-minute quota lets 29 requests through and then waits for the oldest to age out`() = runTest {
        val limiter = RateLimiter(permits = 29, windowMs = 61_000, timeSource = testTimeSource)
        val startedAt = (1..31).map { limiter.acquire(); currentTime }
        assertEquals(List(29) { 0L } + listOf(61_000L, 61_000L), startedAt)
    }
}
