package com.fastpilot.ui.screens.strategy

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.domain.usecase.CalculateCleanPaceUseCase.CleanPaceStats
import com.fastpilot.domain.usecase.GetTyreDegradationUseCase.DegradationCurve
import com.fastpilot.model.Driver
import com.fastpilot.model.PitStop
import com.fastpilot.model.Stint
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StrategyScreen(
    viewModel: StrategyViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Estratégia de Pneus",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // ── Session Picker ──────────────────────────────────────
        if (uiState.sessions.isNotEmpty()) {
            SessionPicker(
                sessions = uiState.sessions,
                selectedSession = uiState.selectedSession,
                onSessionSelected = { viewModel.selectSession(it) }
            )
        }

        Spacer(Modifier.height(8.dp))
        CompoundLegend()
        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text("Carregando estratégia...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (uiState.stintsByDriver.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = TextTertiary,
                        modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhum stint encontrado", color = TextSecondary)
                    Text("Selecione uma sessão de corrida", color = TextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            val driverMap = uiState.drivers.associateBy { it.driverNumber }
            val maxLap = uiState.stintsByDriver.values.flatten()
                .mapNotNull { it.lapEnd }.maxOrNull() ?: 60

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(uiState.stintsByDriver.toList().sortedBy { (driverNum, _) ->
                    driverNum
                }) { (driverNumber, stints) ->
                    val driver = driverMap[driverNumber]
                    StintTimelineRow(driver, stints, maxLap, uiState.degradationByDriver[driverNumber].orEmpty())
                }
                if (uiState.degradationByDriver.isNotEmpty()) {
                    item {
                        Text(
                            "Degradação: aumento do tempo de volta por volta de uso do pneu " +
                                "(regressão linear sobre as voltas limpas de cada stint)",
                            fontSize = 11.sp, color = TextTertiary
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }

                // Clean pace section
                if (uiState.isLoadingMetrics || uiState.cleanPace.isNotEmpty()) {
                    item {
                        CleanPaceHeader(uiState.metricsWithoutRaceControl)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (uiState.isLoadingMetrics) {
                    item {
                        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = F1Red, modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (uiState.cleanPace.isNotEmpty()) {
                    val reference = uiState.cleanPace.first().medianPace
                    itemsIndexed(uiState.cleanPace) { index, stats ->
                        CleanPaceRow(index + 1, stats, driverMap[stats.driverNumber], reference)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // Pit stops section
                if (uiState.pitStops.isNotEmpty()) {
                    item {
                        Text("Pit Stops", style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.pitStops.sortedBy { it.lapNumber }) { pit ->
                        PitStopCard(pit, driverMap[pit.driverNumber])
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun CompoundLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompoundDot("S", TyreSoft)
        CompoundDot("M", TyreMedium)
        CompoundDot("H", TyreHard)
        CompoundDot("I", TyreIntermediate)
        CompoundDot("W", TyreWet)
    }
}

@Composable
private fun CompoundDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun StintTimelineRow(
    driver: Driver?,
    stints: List<Stint>,
    maxLap: Int,
    degradation: List<DegradationCurve>
) {
    val teamColor = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Driver label
                Column(modifier = Modifier.width(50.dp)) {
                    Text(
                        driver?.nameAcronym ?: "#${stints.firstOrNull()?.driverNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = teamColor
                    )
                }
    
                // Timeline bar
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    stints.sortedBy { it.stintNumber }.forEach { stint ->
                        val lapStart = stint.lapStart ?: 0
                        val lapEnd = stint.lapEnd ?: maxLap
                        val fraction = (lapEnd - lapStart + 1).toFloat() / maxLap.coerceAtLeast(1)
    
                        val color = compoundColor(stint.compound)
    
                        Box(
                            modifier = Modifier
                                .weight(fraction.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(color.copy(alpha = 0.8f))
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (fraction > 0.08f) {
                                Text(
                                    "${lapEnd - lapStart + 1}",
                                    fontSize = 9.sp,
                                    color = CarbonBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            if (degradation.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(start = 50.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    degradation.sortedBy { it.stintNumber }.forEach { curve ->
                        DegradationLabel(curve)
                    }
                }
            }
        }
    }
}

@Composable
private fun DegradationLabel(curve: DegradationCurve) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(compoundColor(curve.compound))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${formatSigned(curve.degradationPerLap, 3)} s/volta",
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun CleanPaceHeader(withoutRaceControl: Boolean) {
    Text("Ritmo Limpo", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
    Text(
        "Mediana das voltas representativas: sem volta 1, entrada/saída de box, " +
            "safety car, VSC e bandeira vermelha; 5% mais rápidas e mais lentas descartadas",
        fontSize = 11.sp, color = TextTertiary
    )
    if (withoutRaceControl) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Mensagens da direção de prova indisponíveis: voltas sob safety car podem estar incluídas",
            fontSize = 11.sp, color = SafetyCarAmber
        )
    }
}

@Composable
private fun CleanPaceRow(position: Int, stats: CleanPaceStats, driver: Driver?, reference: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$position", fontSize = 12.sp, color = TextTertiary, modifier = Modifier.width(22.dp))
            Text(
                driver?.nameAcronym ?: "#${stats.driverNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary,
                modifier = Modifier.width(50.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(formatLapTime(stats.medianPace), fontSize = 14.sp, color = TextPrimary,
                    fontWeight = FontWeight.Bold)
                Text("${stats.cleanLapCount}/${stats.totalLapCount} voltas limpas",
                    fontSize = 10.sp, color = TextTertiary)
            }
            Column(horizontalAlignment = Alignment.End) {
                val gap = stats.medianPace - reference
                Text(
                    if (position == 1) "referência" else "${formatSigned(gap, 3)}s",
                    fontSize = 13.sp,
                    color = if (position == 1) DRSGreen else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text("σ ${formatDecimal(stats.consistency, 2)}s", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}

private fun compoundColor(compound: String?) = when (compound?.uppercase()) {
    "SOFT" -> TyreSoft
    "MEDIUM" -> TyreMedium
    "HARD" -> TyreHard
    "INTERMEDIATE" -> TyreIntermediate
    "WET" -> TyreWet
    else -> TextTertiary
}

private fun formatDecimal(value: Double, decimals: Int): String {
    var factor = 1
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToInt()
    val sign = if (value < 0 && scaled != 0) "-" else ""
    return "$sign${scaled / factor}.${(scaled % factor).toString().padStart(decimals, '0')}"
}

private fun formatSigned(value: Double, decimals: Int): String =
    formatDecimal(value, decimals).let { if (it.startsWith("-")) it else "+$it" }

/** 95.123 -> "1:35.123" */
private fun formatLapTime(seconds: Double): String {
    val millis = (seconds * 1000).roundToInt()
    val secs = (millis / 1000) % 60
    return "${millis / 60000}:${secs.toString().padStart(2, '0')}.${(millis % 1000).toString().padStart(3, '0')}"
}

@Composable
private fun PitStopCard(pit: PitStop, driver: Driver?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                driver?.nameAcronym ?: "#${pit.driverNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary,
                modifier = Modifier.width(50.dp)
            )
            Text("Volta ${pit.lapNumber ?: "?"}", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${pit.stopDuration ?: "--"}s",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DRSGreen
                )
                Text("pit stop", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}
