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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.domain.usecase.DeltaPoint
import com.fastpilot.model.Driver
import com.fastpilot.ui.components.DriverSelector
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

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
        Text("Duelo de Pilotos", style = MaterialTheme.typography.headlineSmall,
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
                DriverSelector(
                    label = "Piloto 1",
                    drivers = uiState.drivers,
                    selectedDriver = uiState.driver1,
                    onDriverSelected = { viewModel.selectDriver1(it) },
                    modifier = Modifier.weight(1f)
                )
                Text("VS", color = F1Red, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterVertically))
                DriverSelector(
                    label = "Piloto 2",
                    drivers = uiState.drivers,
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
                    Text("⚔️", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Selecione dois pilotos acima", color = TextSecondary)
                    Text("para comparar o desempenho", color = TextTertiary, fontSize = 12.sp)
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
                    Text("Calculando comparação...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // ── Error ───────────────────────────────────────────────
        uiState.error?.let { error ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(error, Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Duel Results ────────────────────────────────────────
        uiState.duelData?.let { duel ->
            if (duel.cumulativeDelta.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Delta Acumulado", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Positivo = ${uiState.driver1?.nameAcronym ?: "D1"} à frente",
                            fontSize = 11.sp, color = TextTertiary)
                        Spacer(Modifier.height(12.dp))
                        DeltaChart(deltas = duel.cumulativeDelta,
                            modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats comparison
                val avg1 = duel.lapsDriver1.mapNotNull { it.lapDuration }.let { if (it.isEmpty()) 0.0 else it.average() }
                val avg2 = duel.lapsDriver2.mapNotNull { it.lapDuration }.let { if (it.isEmpty()) 0.0 else it.average() }
                val best1 = duel.lapsDriver1.mapNotNull { it.lapDuration }.minOrNull() ?: 0.0
                val best2 = duel.lapsDriver2.mapNotNull { it.lapDuration }.minOrNull() ?: 0.0

                StatsComparisonRow(
                    uiState.driver1?.nameAcronym ?: "D1",
                    uiState.driver2?.nameAcronym ?: "D2",
                    "Ritmo Médio", formatTime(avg1), formatTime(avg2), avg1 < avg2)
                Spacer(Modifier.height(8.dp))
                StatsComparisonRow(
                    uiState.driver1?.nameAcronym ?: "D1",
                    uiState.driver2?.nameAcronym ?: "D2",
                    "Melhor Volta", formatTime(best1), formatTime(best2), best1 < best2)
                Spacer(Modifier.height(8.dp))
                StatsComparisonRow(
                    uiState.driver1?.nameAcronym ?: "D1",
                    uiState.driver2?.nameAcronym ?: "D2",
                    "Voltas Válidas", "${duel.lapsDriver1.size}", "${duel.lapsDriver2.size}",
                    duel.lapsDriver1.size > duel.lapsDriver2.size)
            } else if (!uiState.isComputing && uiState.driver1 != null && uiState.driver2 != null) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp)) {
                    Text("Sem dados de voltas comuns entre os pilotos selecionados",
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
