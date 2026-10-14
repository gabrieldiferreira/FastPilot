package com.fastpilot.ui.screens.telemetry

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.domain.usecase.CalculateTheoreticalBestUseCase.TheoreticalBest
import com.fastpilot.model.CarData
import com.fastpilot.model.Lap
import com.fastpilot.ui.components.DriverSelector
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun TelemetryScreen(
    viewModel: TelemetryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))

        Text(strings.telemetryTitle, style = MaterialTheme.typography.headlineSmall,
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

        // ── Driver Selector ─────────────────────────────────────
        if (uiState.drivers.isNotEmpty()) {
            DriverSelector(
                label = strings.driver,
                drivers = uiState.drivers,
                selectedDriver = uiState.selectedDriver,
                onDriverSelected = { viewModel.selectDriver(it) }
            )
        } else if (!uiState.isLoading && uiState.selectedSession != null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.noDriversForSession,
                    Modifier.padding(16.dp), color = TextTertiary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Lap Selector ────────────────────────────────────────
        if (uiState.laps.isNotEmpty()) {
            Text(strings.laps, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            LapSelectorRow(
                laps = uiState.laps,
                selectedLap = uiState.selectedLap,
                onSelect = { viewModel.selectLap(it) }
            )
        } else if (uiState.selectedDriver != null && !uiState.isLoading) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.noLapsFor(uiState.selectedDriver?.nameAcronym),
                    Modifier.padding(16.dp), color = TextTertiary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Theoretical best ────────────────────────────────────
        uiState.theoreticalBest?.let { best ->
            TheoreticalBestCard(best)
            Spacer(Modifier.height(12.dp))
        }

        // ── Telemetry Charts ────────────────────────────────────
        if (uiState.isLoadingTelemetry) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text(strings.loadingTelemetry, color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (uiState.carData.isNotEmpty()) {
            // Lap info header
            uiState.selectedLap?.let { lap ->
                LapInfoCard(lap)
                Spacer(Modifier.height(12.dp))
            }

            ChartCard(Icons.Default.Speed, strings.speedChart) {
                SpeedChart(data = uiState.carData, modifier = Modifier.fillMaxWidth().height(180.dp))
            }
            Spacer(Modifier.height(12.dp))
            ChartCard(Icons.AutoMirrored.Filled.ShowChart, strings.throttleBrakeChart, legend = {
                LegendDot(ThrottleGreen, strings.throttle)
                LegendDot(BrakeRed, strings.brake)
            }) {
                ThrottleBrakeChart(data = uiState.carData, modifier = Modifier.fillMaxWidth().height(120.dp))
            }
            Spacer(Modifier.height(12.dp))
            ChartCard(Icons.Default.Settings, strings.gear) {
                GearChart(data = uiState.carData, modifier = Modifier.fillMaxWidth().height(100.dp))
            }
        } else if (uiState.selectedDriver == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null,
                        tint = TextTertiary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(strings.selectDriverAbove, color = TextSecondary)
                    Text(strings.toSeeTelemetry, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = F1Red)
            }
        }

        uiState.error?.let { error ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.error(error), Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun LapInfoCard(lap: Lap) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            LapStat(strings.lapLabel, strings.lapShort(lap.lapNumber))
            LapStat(strings.timeLabel, lap.lapDuration?.let { formatDuration(it) } ?: "--")
            LapStat("S1", lap.sector1?.let { formatDuration(it) } ?: "--")
            LapStat("S2", lap.sector2?.let { formatDuration(it) } ?: "--")
            LapStat("S3", lap.sector3?.let { formatDuration(it) } ?: "--")
        }
    }
}

@Composable
private fun LapStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextTertiary)
        Text(value, fontSize = 13.sp, color = DRSGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LapSelectorRow(laps: List<Lap>, selectedLap: Lap?, onSelect: (Lap) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(laps) { lap ->
            val isSelected = lap.lapNumber == selectedLap?.lapNumber
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(lap) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.lapShort(lap.lapNumber), fontSize = 12.sp)
                        Text(lap.lapDuration?.let { formatDuration(it) } ?: "--",
                            fontSize = 10.sp, color = if (isSelected) TextPrimary else TextTertiary)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = F1Red.copy(alpha = 0.3f), containerColor = CardSurface)
            )
        }
    }
}

/**
 * Best S1 + best S2 + best S3 of the session against the best lap actually driven:
 * the gap shows how much time the driver left by not linking their best sectors.
 */
@Composable
private fun TheoreticalBestCard(best: TheoreticalBest) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = TextSecondary,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.theoreticalLap, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text(strings.theoreticalLapHint, fontSize = 11.sp, color = TextTertiary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(strings.theoretical, fontSize = 10.sp, color = TextTertiary)
                    Text(formatLapTime(best.theoreticalBestTime), fontSize = 22.sp,
                        color = GearPurple, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(strings.bestActualLap, fontSize = 10.sp, color = TextTertiary)
                    Text(formatLapTime(best.actualBestTime), fontSize = 16.sp,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.difference, fontSize = 10.sp, color = TextTertiary)
                    Text("+${formatDuration(best.gap)}s", fontSize = 16.sp,
                        color = SafetyCarAmber, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SectorStat("S1", best.bestSector1, best.bestSector1Lap)
                SectorStat("S2", best.bestSector2, best.bestSector2Lap)
                SectorStat("S3", best.bestSector3, best.bestSector3Lap)
            }
        }
    }
}

@Composable
private fun SectorStat(label: String, time: Double, lapNumber: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextTertiary)
        Text(formatDuration(time), fontSize = 13.sp, color = GearPurple, fontWeight = FontWeight.Bold)
        Text(strings.lap(lapNumber).lowercase(), fontSize = 10.sp, color = TextTertiary)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = TextTertiary)
    }
}

@Composable
private fun ChartCard(
    icon: ImageVector,
    title: String,
    legend: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Text(title, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f))
                legend()
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SpeedChart(data: List<CarData>, modifier: Modifier = Modifier) {
    val speeds = data.mapNotNull { it.speed }
    if (speeds.isEmpty()) return
    val maxSpeed = speeds.max().toFloat()
    val minSpeed = speeds.min().toFloat()

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val w = size.width; val h = size.height
        val stepX = w / (speeds.size - 1).coerceAtLeast(1)
        val range = (maxSpeed - minSpeed).coerceAtLeast(1f)
        val path = Path()
        speeds.forEachIndexed { i, speed ->
            val x = i * stepX; val y = h - ((speed - minSpeed) / range * h)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, SpeedBlue, style = Stroke(width = 2.5f))
    }
}

@Composable
private fun ThrottleBrakeChart(data: List<CarData>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val w = size.width; val h = size.height
        val stepX = w / (data.size - 1).coerceAtLeast(1)
        val throttlePath = Path(); val brakePath = Path()
        data.forEachIndexed { i, d ->
            val x = i * stepX
            val ty = h - ((d.throttle ?: 0) / 100f * h * 0.5f)
            val by = h * 0.5f + ((d.brake ?: 0) / 100f * h * 0.5f)
            if (i == 0) { throttlePath.moveTo(x, ty); brakePath.moveTo(x, by) }
            else { throttlePath.lineTo(x, ty); brakePath.lineTo(x, by) }
        }
        drawPath(throttlePath, ThrottleGreen.copy(alpha = 0.8f), style = Stroke(width = 2f))
        drawPath(brakePath, BrakeRed.copy(alpha = 0.8f), style = Stroke(width = 2f))
        drawLine(DividerColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
    }
}

@Composable
private fun GearChart(data: List<CarData>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val w = size.width; val h = size.height
        val stepX = w / (data.size - 1).coerceAtLeast(1)
        val path = Path()
        data.forEachIndexed { i, d ->
            val gear = (d.gear ?: 0).coerceIn(0, 8)
            val x = i * stepX; val y = h - (gear / 8f * h)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, GearPurple, style = Stroke(width = 2f))
    }
}

/** 95.123 -> "1:35.123" */
private fun formatLapTime(seconds: Double): String {
    val millis = (seconds * 1000).roundToInt()
    val secs = (millis / 1000) % 60
    return "${millis / 60000}:${secs.toString().padStart(2, '0')}.${(millis % 1000).toString().padStart(3, '0')}"
}

private fun formatDuration(value: Double): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 1000).toInt()
    return "$intPart.${decimalPart.toString().padStart(3, '0')}"
}
