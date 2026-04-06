package com.fastpilot.ui.screens.strategy

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.model.Driver
import com.fastpilot.model.PitStop
import com.fastpilot.model.Stint
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

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
                    Text("🏁", fontSize = 40.sp)
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
                    StintTimelineRow(driver, stints, maxLap)
                }
                item { Spacer(Modifier.height(16.dp)) }

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
private fun StintTimelineRow(driver: Driver?, stints: List<Stint>, maxLap: Int) {
    val teamColor = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

                    val color = when (stint.compound?.uppercase()) {
                        "SOFT" -> TyreSoft
                        "MEDIUM" -> TyreMedium
                        "HARD" -> TyreHard
                        "INTERMEDIATE" -> TyreIntermediate
                        "WET" -> TyreWet
                        else -> TextTertiary
                    }

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
    }
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
