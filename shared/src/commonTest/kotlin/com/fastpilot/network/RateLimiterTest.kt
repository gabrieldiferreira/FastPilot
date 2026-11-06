package com.fastpilot.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    @Test
    fun `the newest waiting request is served first so a new screen does not wait for an old backlog`() = runTest {
        val limiter = RateLimiter(permits = 1, windowMs = 1_000, timeSource = testTimeSource)
        limiter.acquire() // quota used at 0
        val startedAt = mutableMapOf<String, Long>()
        launch { limiter.acquire(); startedAt["old screen"] = currentTime }
        launch { delay(100); limiter.acquire(); startedAt["new screen"] = currentTime }
        testScheduler.advanceUntilIdle()
        assertEquals(1_000L, startedAt["new screen"])
        assertEquals(2_000L, startedAt["old screen"])
    }
}
