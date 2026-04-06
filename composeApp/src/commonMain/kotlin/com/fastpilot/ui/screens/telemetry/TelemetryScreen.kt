package com.fastpilot.ui.screens.telemetry

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.model.CarData
import com.fastpilot.model.Lap
import com.fastpilot.ui.components.DriverSelector
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

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

        Text("Telemetria", style = MaterialTheme.typography.headlineSmall,
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
                label = "Piloto",
                drivers = uiState.drivers,
                selectedDriver = uiState.selectedDriver,
                onDriverSelected = { viewModel.selectDriver(it) }
            )
        } else if (!uiState.isLoading && uiState.selectedSession != null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(10.dp)) {
                Text("Nenhum piloto disponível para esta sessão",
                    Modifier.padding(16.dp), color = TextTertiary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Lap Selector ────────────────────────────────────────
        if (uiState.laps.isNotEmpty()) {
            Text("Voltas", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            LapSelectorRow(
                laps = uiState.laps,
                selectedLap = uiState.selectedLap,
                onSelect = { viewModel.selectLap(it) }
            )
        } else if (uiState.selectedDriver != null && !uiState.isLoading) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(10.dp)) {
                Text("Nenhuma volta registrada para ${uiState.selectedDriver?.nameAcronym ?: "este piloto"}",
                    Modifier.padding(16.dp), color = TextTertiary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Telemetry Charts ────────────────────────────────────
        if (uiState.isLoadingTelemetry) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text("Carregando telemetria...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (uiState.carData.isNotEmpty()) {
            // Lap info header
            uiState.selectedLap?.let { lap ->
                LapInfoCard(lap)
                Spacer(Modifier.height(12.dp))
            }

            ChartCard("🏎️ Velocidade (km/h)") {
                SpeedChart(data = uiState.carData, modifier = Modifier.fillMaxWidth().height(180.dp))
            }
            Spacer(Modifier.height(12.dp))
            ChartCard("🟢 Throttle / 🔴 Freio") {
                ThrottleBrakeChart(data = uiState.carData, modifier = Modifier.fillMaxWidth().height(120.dp))
            }
            Spacer(Modifier.height(12.dp))
            ChartCard("⚙️ Marcha") {
                GearChart(data = uiState.carData, modifier = Modifier.fillMaxWidth().height(100.dp))
            }
        } else if (uiState.selectedDriver == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Selecione um piloto acima", color = TextSecondary)
                    Text("para ver a telemetria", color = TextTertiary, fontSize = 12.sp)
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
                Text(error, Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
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
            LapStat("Volta", "L${lap.lapNumber}")
            LapStat("Tempo", lap.lapDuration?.let { formatDuration(it) } ?: "--")
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
                        Text("L${lap.lapNumber}", fontSize = 12.sp)
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

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

private fun formatDuration(value: Double): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 1000).toInt()
    return "$intPart.${decimalPart.toString().padStart(3, '0')}"
}
