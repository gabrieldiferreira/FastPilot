package com.fastpilot.ui.screens.duel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import com.fastpilot.domain.usecase.DeltaPoint
import com.fastpilot.domain.usecase.LapDelta
import com.fastpilot.domain.usecase.LapDeltaPoint
import com.fastpilot.model.Driver
import com.fastpilot.ui.components.DriverSelector
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun DuelScreen(
    viewModel: DuelViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))
        Text(strings.duelTitle, style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        // ── Session Picker ──────────────────────────────────────
        if (uiState.sessions.isNotEmpty()) {
            SessionPicker(
                sessions = uiState.sessions,
                selectedSession = uiState.selectedSession,
                onSessionSelected = { viewModel.selectSession(it) }
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Driver Selectors ────────────────────────────────────
        if (uiState.drivers.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // A driver picked on one side is not offered on the other: a duel needs two drivers
                DriverSelector(
                    label = strings.driverN(1),
                    drivers = uiState.drivers.filter { it.driverNumber != uiState.driver2?.driverNumber },
                    selectedDriver = uiState.driver1,
                    onDriverSelected = { viewModel.selectDriver1(it) },
                    modifier = Modifier.weight(1f)
                )
                Text("VS", color = F1Red, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterVertically))
                DriverSelector(
                    label = strings.driverN(2),
                    drivers = uiState.drivers.filter { it.driverNumber != uiState.driver1?.driverNumber },
                    selectedDriver = uiState.driver2,
                    onDriverSelected = { viewModel.selectDriver2(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Prompt to select ────────────────────────────────────
        if (uiState.driver1 == null || uiState.driver2 == null) {
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(strings.selectTwoDrivers, color = TextSecondary)
                    Text(strings.toComparePerformance, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Computing indicator ─────────────────────────────────
        if (uiState.isComputing) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text(strings.computingComparison, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // ── Error ───────────────────────────────────────────────
        uiState.error?.let { error ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.error(error), Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Duel Results ────────────────────────────────────────
        uiState.duelData?.let { duel ->
            if (duel.cumulativeDelta.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(strings.cumulativeDelta, color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(strings.positiveMeansAhead(uiState.driver1?.nameAcronym ?: "D1"),
                            fontSize = 11.sp, color = TextTertiary)
                        Spacer(Modifier.height(12.dp))
                        DeltaChart(deltas = duel.cumulativeDelta,
                            modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Where time is gained or lost within the fastest laps (TCC I §4.7)
                LapDeltaCard(
                    lapDelta = uiState.lapDelta,
                    isComputing = uiState.isComputingLapDelta,
                    unavailable = uiState.lapDeltaUnavailable,
                    driver1 = uiState.driver1,
                    driver2 = uiState.driver2
                )

                Spacer(Modifier.height(16.dp))

                // Stats comparison
                val avg1 = duel.lapsDriver1.mapNotNull { it.lapDuration }.let { if (it.isEmpty()) 0.0 else it.average() }
                val avg2 = duel.lapsDriver2.mapNotNull { it.lapDuration }.let { if (it.isEmpty()) 0.0 else it.average() }
                val best1 = duel.lapsDriver1.mapNotNull { it.lapDuration }.minOrNull() ?: 0.0
                val best2 = duel.lapsDriver2.mapNotNull { it.lapDuration }.minOrNull() ?: 0.0

                StatsComparisonRow(
                    uiState.driver1?.nameAcronym ?: "D1",
                    uiState.driver2?.nameAcronym ?: "D2",
                    strings.averagePace, formatTime(avg1), formatTime(avg2), avg1 < avg2)
                Spacer(Modifier.height(8.dp))
                StatsComparisonRow(
                    uiState.driver1?.nameAcronym ?: "D1",
                    uiState.driver2?.nameAcronym ?: "D2",
                    strings.bestLap, formatTime(best1), formatTime(best2), best1 < best2)
                Spacer(Modifier.height(8.dp))
                StatsComparisonRow(
                    uiState.driver1?.nameAcronym ?: "D1",
                    uiState.driver2?.nameAcronym ?: "D2",
                    strings.validLaps, "${duel.lapsDriver1.size}", "${duel.lapsDriver2.size}",
                    duel.lapsDriver1.size > duel.lapsDriver2.size)
            } else if (!uiState.isComputing && uiState.driver1 != null && uiState.driver2 != null) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(strings.noCommonLaps,
                        Modifier.padding(16.dp), color = TextTertiary, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun DeltaChart(deltas: List<DeltaPoint>, modifier: Modifier = Modifier) {
    val values = deltas.map { it.delta }
    val maxAbs = values.maxOfOrNull { kotlin.math.abs(it) }?.toFloat()?.coerceAtLeast(0.1f) ?: 1f

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val w = size.width; val h = size.height; val midY = h / 2
        val stepX = w / (deltas.size - 1).coerceAtLeast(1)

        drawLine(DividerColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)

        val path = Path()
        deltas.forEachIndexed { i, dp ->
            val x = i * stepX
            val y = midY - (dp.delta.toFloat() / maxAbs * midY * 0.9f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, DRSGreen, style = Stroke(width = 2.5f))
    }
}

@Composable
private fun LapDeltaCard(lapDelta: LapDelta?, isComputing: Boolean, unavailable: Boolean, driver1: Driver?, driver2: Driver?) {
    if (lapDelta == null && !isComputing && !unavailable) return
    val name1 = driver1?.nameAcronym ?: "D1"
    val name2 = driver2?.nameAcronym ?: "D2"
    val color1 = driver1?.teamColour?.let { TeamColors.fromHex(it) } ?: DRSGreen
    val color2 = driver2?.teamColour?.let { TeamColors.fromHex(it) } ?: F1Red

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(strings.fastestLapDelta, color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            if (lapDelta == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    if (unavailable) Text(strings.positionDataUnavailable, color = TextTertiary, fontSize = 12.sp)
                    else CircularProgressIndicator(color = F1Red, modifier = Modifier.size(24.dp))
                }
                return@Column
            }
            Text(
                strings.lapVsLap(lapDelta.referenceLapNumber, name1, lapDelta.comparedLapNumber, name2),
                fontSize = 11.sp, color = TextTertiary
            )
            Spacer(Modifier.height(12.dp))
            LapDeltaChart(lapDelta, color1, color2, Modifier.fillMaxWidth().height(180.dp))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("0 m", color = TextTertiary, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text("${lapDelta.lapLength.toInt()} m", color = TextTertiary, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            val gap = lapDelta.finalGap
            Text(
                when {
                    gap > 0 -> strings.fasterBy(name1, formatGap(gap))
                    gap < 0 -> strings.fasterBy(name2, formatGap(-gap))
                    else -> strings.sameLapTime
                },
                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
            )
        }
    }
}

/**
 * Delta along the lap distance; area above zero in driver 1's colour, below in driver 2's.
 * The drawn line is smoothed over 5 samples (~1.3 s) to hide position-sampling jitter;
 * the endpoints keep their exact values (0 at the line, official gap at the end).
 */
@Composable
private fun LapDeltaChart(lapDelta: LapDelta, color1: Color, color2: Color, modifier: Modifier = Modifier) {
    val points = remember(lapDelta) { smooth(lapDelta.points, window = 5) }
    val maxAbs = points.maxOf { kotlin.math.abs(it.delta) }.toFloat().coerceAtLeast(0.05f)

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val w = size.width; val h = size.height; val midY = h / 2
        fun x(distance: Double) = (distance / lapDelta.lapLength).toFloat() * w
        fun y(delta: Double) = midY - (delta.toFloat() / maxAbs * midY * 0.9f)

        val line = Path()
        points.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(p.distance), y(p.delta)) else line.lineTo(x(p.distance), y(p.delta)) }

        val area = Path().apply {
            addPath(line)
            lineTo(x(points.last().distance), midY)
            lineTo(x(points.first().distance), midY)
            close()
        }
        clipRect(bottom = midY) { drawPath(area, color1.copy(alpha = 0.25f)) }
        clipRect(top = midY) { drawPath(area, color2.copy(alpha = 0.25f)) }

        drawLine(DividerColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
        drawPath(line, TextPrimary, style = Stroke(width = 2f))
    }
}

private fun smooth(points: List<LapDeltaPoint>, window: Int): List<LapDeltaPoint> {
    val half = window / 2
    return points.mapIndexed { i, p ->
        if (i == 0 || i == points.lastIndex) p
        else {
            val from = maxOf(1, i - half)
            val to = minOf(points.lastIndex - 1, i + half)
            p.copy(delta = (from..to).sumOf { points[it].delta } / (to - from + 1))
        }
    }
}

private fun formatGap(seconds: Double): String {
    val millis = (seconds * 1000).roundToInt()
    return "${millis / 1000}.${(millis % 1000).toString().padStart(3, '0')}s"
}

@Composable
private fun StatsComparisonRow(
    name1: String, name2: String, label: String,
    value1: String, value2: String, driver1Better: Boolean
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = TextTertiary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(name1, fontSize = 11.sp, color = TextTertiary)
                    Text(value1, fontWeight = FontWeight.Bold,
                        color = if (driver1Better) DRSGreen else TextSecondary, fontSize = 16.sp)
                }
                Text("vs", color = TextTertiary, fontSize = 11.sp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(name2, fontSize = 11.sp, color = TextTertiary)
                    Text(value2, fontWeight = FontWeight.Bold,
                        color = if (!driver1Better) DRSGreen else TextSecondary, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    if (seconds <= 0 || seconds.isNaN()) return "--"
    val mins = (seconds / 60).toInt()
    val secs = seconds % 60
    val secsInt = secs.toInt()
    val secsDecimal = ((secs - secsInt) * 1000).toInt()
    val secsFormatted = "${secsInt.toString().padStart(2, '0')}.${secsDecimal.toString().padStart(3, '0')}"
    return if (mins > 0) "$mins:$secsFormatted"
    else "$secsInt.${secsDecimal.toString().padStart(3, '0')}"
}
