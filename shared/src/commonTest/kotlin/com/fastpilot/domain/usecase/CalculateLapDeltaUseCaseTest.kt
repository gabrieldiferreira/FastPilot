package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.Location
import kotlinx.datetime.Instant
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.microseconds

class CalculateLapDeltaUseCaseTest {

    private val useCase = CalculateLapDeltaUseCase()

    // OpenF1 date format
    private val lapStart = "2023-03-05T16:00:00.000000+00:00"
    private val start = Instant.parse(lapStart)

    private fun lap(driver: Int, duration: Double) = Lap(
        driverNumber = driver, lapNumber = 10, lapDuration = duration, dateStart = lapStart, sessionKey = 1
    )

    /**
     * Samples a car driving [path] (metres, parameterised by fraction of the lap 0..1)
     * at [interval] seconds, starting [offset] seconds after the line; progress(t) in 0..1.
     */
    private fun samples(
        driver: Int, duration: Double, interval: Double = 0.27, offset: Double = 0.1,
        progress: (Double) -> Double = { it / duration },
        path: (Double) -> Pair<Double, Double>
    ): List<Location> {
        val out = mutableListOf<Location>()
        var t = offset
        while (t <= duration) {
            val (x, y) = path(progress(t))
            out += Location(
                driverNumber = driver,
                x = (x * 10).roundToInt(), y = (y * 10).roundToInt(), z = 0,  // decimetres
                date = (start + (t * 1_000_000).toLong().microseconds).toString()
            )
            t += interval
        }
        return out
    }

    /** 2 km oval-ish closed loop. */
    private val circuit: (Double) -> Pair<Double, Double> = { f ->
        val angle = 2 * PI * f
        Pair(400 * cos(angle), 236.6 * sin(angle))
    }

    @Test
    fun `identical laps have zero delta`() {
        val delta = assertNotNull(useCase(lap(1, 90.0), samples(1, 90.0, path = circuit), lap(44, 90.0), samples(44, 90.0, path = circuit)))

        assertTrue(delta.points.all { abs(it.delta) < 0.05 }, "max ${delta.points.maxOf { abs(it.delta) }}")
        assertEquals(0.0, delta.finalGap, 1e-9)
    }

    @Test
    fun `uniformly slower driver loses time linearly and ends at the lap time gap`() {
        val delta = assertNotNull(useCase(lap(1, 90.0), samples(1, 90.0, path = circuit), lap(44, 91.8), samples(44, 91.8, path = circuit)))

        // 2% slower everywhere: delta at fraction f of the lap is 1.8 * f
        for (p in delta.points.drop(1).dropLast(1)) {
            val expected = 1.8 * p.distance / delta.lapLength
            assertEquals(expected, p.delta, 0.06, "at ${p.distance} m")
        }
        assertEquals(1.8, delta.points.last().delta, 1e-9)
        assertEquals(delta.lapLength, delta.points.last().distance)
    }

    @Test
    fun `samples taken at different instants are synchronised by interpolation`() {
        val delta = assertNotNull(
            useCase(
                lap(1, 90.0), samples(1, 90.0, interval = 0.27, offset = 0.05, path = circuit),
                lap(44, 90.0), samples(44, 90.0, interval = 0.31, offset = 0.2, path = circuit)
            )
        )

        assertTrue(delta.points.all { abs(it.delta) < 0.05 }, "max ${delta.points.maxOf { abs(it.delta) }}")
    }

    @Test
    fun `a different racing line is matched to the same part of the track`() {
        // Compared driver runs 3 m wide of the reference line all lap, same timing
        val wide: (Double) -> Pair<Double, Double> = { f ->
            val angle = 2 * PI * f
            Pair(403 * cos(angle), 239.6 * sin(angle))
        }
        val delta = assertNotNull(useCase(lap(1, 90.0), samples(1, 90.0, path = circuit), lap(44, 90.0), samples(44, 90.0, path = wide)))

        assertTrue(delta.points.all { abs(it.delta) < 0.1 }, "max ${delta.points.maxOf { abs(it.delta) }}")
    }

    @Test
    fun `shows where time is gained within the lap`() {
        // Compared driver is slower in the first half, faster in the second, same lap time
        val delta = assertNotNull(
            useCase(
                lap(1, 90.0), samples(1, 90.0, path = circuit),
                lap(44, 90.0), samples(44, 90.0, path = circuit, progress = { t ->
                    val u = t / 90.0
                    if (u < 0.5) u * 0.96 else 0.48 + (u - 0.5) * 1.04
                })
            )
        )

        // Loss grows at 3.75 s per lap fraction until the speed change at 48% (angle), then shrinks
        val worst = delta.points.maxBy { it.delta }
        assertEquals(0.48, worst.distance / delta.lapLength, 0.02)
        assertEquals(1.8, worst.delta, 0.03)
        assertEquals(0.0, delta.points.last().delta, 1e-9)
    }

    @Test
    fun `a track that crosses itself is not confused at the crossover`() {
        // Figure-eight (lemniscate of Gerono), like Suzuka: both legs pass through (0, 0)
        val figureEight: (Double) -> Pair<Double, Double> = { f ->
            val theta = 2 * PI * f + PI / 4
            Pair(500 * cos(theta), 250 * sin(2 * theta))
        }
        // Compared driver takes a line 3 m to the side, so near the crossover some of its
        // samples are closer to the other leg than to its own
        val offsetLine: (Double) -> Pair<Double, Double> = { f ->
            val (x0, y0) = figureEight(f)
            val (x1, y1) = figureEight(f + 1e-4)
            val norm = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0))
            Pair(x0 - 3 * (y1 - y0) / norm, y0 + 3 * (x1 - x0) / norm)
        }
        val delta = assertNotNull(
            useCase(lap(1, 90.0), samples(1, 90.0, path = figureEight), lap(44, 91.8), samples(44, 91.8, path = offsetLine))
        )

        // A uniformly slower driver loses time monotonically; snapping to the other leg would break it
        val deltas = delta.points.map { it.delta }
        assertTrue(deltas.zipWithNext().all { (a, b) -> b >= a - 0.02 }, "non-monotonic: $deltas")
        assertEquals(1.8, deltas.last(), 1e-9)
    }

    @Test
    fun `returns null without location data`() {
        assertNull(useCase(lap(1, 90.0), emptyList(), lap(44, 90.0), samples(44, 90.0, path = circuit)))
    }
}
