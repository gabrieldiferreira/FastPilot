package com.fastpilot.ui.perf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class LoadBenchmarkConfigTest {

    @Test
    fun `parses the launch spec`() {
        val config = LoadBenchmark.Config.parse("off:2,on:1,cooldown:5,timeout:30,idle:10")

        assertEquals(LoadBenchmark.Config(2, 1, 5.seconds, 30.seconds, 10.seconds), config)
    }

    @Test
    fun `missing or malformed entries keep the defaults`() {
        assertEquals(LoadBenchmark.Config(), LoadBenchmark.Config.parse(""))
        assertEquals(LoadBenchmark.Config(), LoadBenchmark.Config.parse(null))
        assertEquals(LoadBenchmark.Config().copy(runsWithPolling = 0), LoadBenchmark.Config.parse("on:0,off:x,cooldown"))
    }
}
