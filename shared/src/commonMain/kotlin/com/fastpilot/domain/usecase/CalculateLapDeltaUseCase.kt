package com.fastpilot.domain.usecase

import com.fastpilot.model.Lap
import com.fastpilot.model.Location
import kotlinx.datetime.Instant
import kotlin.math.sqrt

/** Time gap of the compared driver at a given point of the lap (positive = compared driver behind). */
data class LapDeltaPoint(
    /** Distance from the start of the lap along the reference driver's line, in metres. */
    val distance: Double,
    val delta: Double
)

data class LapDelta(
    val referenceDriver: Int,
    val comparedDriver: Int,
    val referenceLapNumber: Int,
    val comparedLapNumber: Int,
    val points: List<LapDeltaPoint>,
    /** Length of the reference driver's traced line, in metres. */
    val lapLength: Double,
    /** Official lap time difference (compared - reference), the value the curve ends at. */
    val finalGap: Double
)

/**
 * Where each driver gains or loses time during a lap (TCC I, §4.7).
 *
 * OpenF1 /location samples (~3.7 Hz, coordinates in decimetres) are turned into a
 * trace per driver. Each sample of the compared driver is projected onto the reference
 * driver's line, so both are measured at the same part of the track, and the compared
 * driver's time is linearly interpolated at every reference sample, since the two cars
 * are never sampled at exactly the same instant.
 */
class CalculateLapDeltaUseCase {

    private class TracePoint(val time: Double, val x: Double, val y: Double, val z: Double)

    /** Projection of a sample on the reference line; [outside] when it falls before its start or past its end. */
    private class Projection(val segment: Int, val distance: Double, val outside: Boolean)

    companion object {
        private const val DECIMETRES_PER_METRE = 10.0

        /** How many reference segments ahead to search (~10 s of driving), keeps matching monotonic. */
        private const val SEARCH_WINDOW = 40
    }

    operator fun invoke(
        referenceLap: Lap,
        referenceLocations: List<Location>,
        comparedLap: Lap,
        comparedLocations: List<Location>
    ): LapDelta? {
        val reference = trace(referenceLap, referenceLocations) ?: return null
        val compared = trace(comparedLap, comparedLocations) ?: return null
        val refDuration = referenceLap.lapDuration ?: return null
        val cmpDuration = comparedLap.lapDuration ?: return null

        val refDistances = cumulativeDistances(reference)
        val lapLength = refDistances.last()

        // Compared driver's (distance on reference line, time) pairs, distance non-decreasing.
        // Samples beyond either end of the reference trace can't be placed on it and are dropped.
        val cmpDistances = mutableListOf<Double>()
        val cmpTimes = mutableListOf<Double>()
        var segment = 0
        for (point in compared) {
            val projection = project(point, reference, refDistances, from = segment)
            segment = projection.segment
            if (projection.outside) continue
            cmpDistances += maxOf(projection.distance, cmpDistances.lastOrNull() ?: 0.0)
            cmpTimes += point.time
        }
        if (cmpDistances.size < 2) return null
        val cmpDistance = cmpDistances.toDoubleArray()

        // Both cars start the lap on the same line at t = 0 and finish it at their lap times
        val points = mutableListOf(LapDeltaPoint(0.0, 0.0))
        for ((i, refPoint) in reference.withIndex()) {
            val d = refDistances[i]
            if (d < cmpDistance.first() || d > cmpDistance.last()) continue
            val cmpTime = interpolate(d, cmpDistance, cmpTimes)
            points += LapDeltaPoint(d, cmpTime - refPoint.time)
        }
        val finalGap = cmpDuration - refDuration
        points += LapDeltaPoint(lapLength, finalGap)

        return LapDelta(
            referenceDriver = referenceLap.driverNumber,
            comparedDriver = comparedLap.driverNumber,
            referenceLapNumber = referenceLap.lapNumber,
            comparedLapNumber = comparedLap.lapNumber,
            points = points,
            lapLength = lapLength,
            finalGap = finalGap
        )
    }

    /** Samples inside the lap, as seconds since the lap start and coordinates in metres. */
    private fun trace(lap: Lap, locations: List<Location>): List<TracePoint>? {
        val start = lap.dateStart?.let(Instant::parse) ?: return null
        val duration = lap.lapDuration ?: return null
        val points = locations
            .filter { it.driverNumber == lap.driverNumber && it.x != null && it.y != null && it.date != null }
            .map { loc ->
                TracePoint(
                    time = (Instant.parse(loc.date!!) - start).inWholeMicroseconds / 1_000_000.0,
                    x = loc.x!! / DECIMETRES_PER_METRE,
                    y = loc.y!! / DECIMETRES_PER_METRE,
                    z = (loc.z ?: 0) / DECIMETRES_PER_METRE
                )
            }
            .filter { it.time in 0.0..duration }
            .sortedBy { it.time }
        return points.takeIf { it.size >= 2 }
    }

    private fun cumulativeDistances(trace: List<TracePoint>): DoubleArray {
        val distances = DoubleArray(trace.size)
        for (i in 1 until trace.size) {
            distances[i] = distances[i - 1] + distance(trace[i - 1], trace[i])
        }
        return distances
    }

    /**
     * Nearest point on the reference polyline, searching only a window of segments ahead of
     * the previous match so a sample never snaps to another part of the track that passes nearby.
     */
    private fun project(point: TracePoint, reference: List<TracePoint>, refDistances: DoubleArray, from: Int): Projection {
        var best = Projection(from, refDistances[from], outside = false)
        var bestDistSq = Double.MAX_VALUE
        val last = minOf(reference.size - 2, from + SEARCH_WINDOW)
        for (s in from..last) {
            val a = reference[s]
            val b = reference[s + 1]
            val abx = b.x - a.x; val aby = b.y - a.y; val abz = b.z - a.z
            val lengthSq = abx * abx + aby * aby + abz * abz
            val rawT = if (lengthSq == 0.0) 0.0
            else ((point.x - a.x) * abx + (point.y - a.y) * aby + (point.z - a.z) * abz) / lengthSq
            val t = rawT.coerceIn(0.0, 1.0)
            val px = a.x + t * abx; val py = a.y + t * aby; val pz = a.z + t * abz
            val distSq = (point.x - px) * (point.x - px) + (point.y - py) * (point.y - py) + (point.z - pz) * (point.z - pz)
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                best = Projection(
                    segment = s,
                    distance = refDistances[s] + t * (refDistances[s + 1] - refDistances[s]),
                    outside = (s == 0 && rawT < 0.0) || (s == reference.size - 2 && rawT > 1.0)
                )
            }
        }
        return best
    }

    /** Linear interpolation of ys at x; xs must be non-decreasing and contain x within its range. */
    private fun interpolate(x: Double, xs: DoubleArray, ys: List<Double>): Double {
        val hi = xs.indexOfFirst { it >= x }
        if (hi <= 0) return ys[0]
        // xs[lo] < x <= xs[hi], so the span is never zero
        val lo = hi - 1
        return ys[lo] + (ys[hi] - ys[lo]) * (x - xs[lo]) / (xs[hi] - xs[lo])
    }

    private fun distance(a: TracePoint, b: TracePoint): Double {
        val dx = b.x - a.x; val dy = b.y - a.y; val dz = b.z - a.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
