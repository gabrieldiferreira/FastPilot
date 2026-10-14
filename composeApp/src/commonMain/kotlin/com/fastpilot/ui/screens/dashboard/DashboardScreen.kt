package com.fastpilot.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import com.fastpilot.domain.usecase.PositionWithDriver
import com.fastpilot.domain.usecase.RaceEvent
import com.fastpilot.model.Driver
import com.fastpilot.model.Interval
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Session Picker ────────────────────────────────────────
        if (uiState.sessions.isNotEmpty()) {
            SessionPicker(
                sessions = uiState.sessions,
                selectedSession = uiState.currentSession,
                onSessionSelected = { viewModel.selectSession(it) }
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Header ────────────────────────────────────────────────
        LiveTimingHeader(
            sessionName = uiState.currentSession?.sessionName ?: "Carregando...",
            circuitName = uiState.currentSession?.circuitShortName ?: "",
            countryName = uiState.currentSession?.countryName ?: "",
            isLive = uiState.isLive
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text("Carregando dados...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (uiState.error != null) {
            ErrorCard(uiState.error!!)
        } else if (uiState.positions.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma posição encontrada", color = TextSecondary)
                    Text("Selecione outra sessão acima", color = TextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Race Control banner
                if (uiState.raceControlMessages.isNotEmpty()) {
                    item { RaceControlBanner(uiState.raceControlMessages.lastOrNull()) }
                }

                // Position cards
                items(uiState.positions) { posWithDriver ->
                    PositionCard(
                        positionWithDriver = posWithDriver,
                        interval = uiState.intervals[posWithDriver.position.driverNumber]
                    )
                }

                // Pit stops and overtakes
                if (uiState.events.isNotEmpty()) {
                    val drivers = uiState.positions.mapNotNull { it.driver }.associateBy { it.driverNumber }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Eventos", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.events) { event -> RaceEventRow(event, drivers) }
                }

                // Race Control Feed
                if (uiState.raceControlMessages.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Race Control", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.raceControlMessages.takeLast(10).reversed()) { msg ->
                        RaceControlMessageCard(msg)
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun LiveTimingHeader(sessionName: String, circuitName: String, countryName: String, isLive: Boolean) {
    // Pulsing animation for the live indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val dotColor = if (isLive) DRSGreen else TextTertiary
    val labelText = if (isLive) "AO VIVO" else "HISTÓRICO"
    val labelColor = if (isLive) DRSGreen else TextTertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .graphicsLayer { alpha = if (isLive) pulseAlpha else 1f }
                    .background(dotColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(labelText, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sessionName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("$circuitName • $countryName", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PositionCard(positionWithDriver: PositionWithDriver, interval: Interval?) {
    val driver = positionWithDriver.driver
    val position = positionWithDriver.position
    val teamColor = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(
                    when (position.position) { 1 -> F1Red; 2, 3 -> DarkSurface; else -> CardSurfaceHover }
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("${position.position}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver?.nameAcronym ?: "P${position.driverNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(driver?.teamName ?: "", fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(interval?.intervalDisplay ?: "--", fontSize = 14.sp, color = DRSGreen, fontWeight = FontWeight.Medium)
                Text("Gap: ${interval?.gapToLeaderDisplay ?: "--"}", fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun RaceControlBanner(message: RaceControlMessage?) {
    if (message == null) return
    val bgColor = when (message.flag) {
        "YELLOW", "DOUBLE YELLOW" -> FlagYellow.copy(alpha = 0.2f); "RED" -> F1Red.copy(alpha = 0.2f)
        "GREEN" -> DRSGreen.copy(alpha = 0.2f); else -> CardSurface
    }
    val textColor = when (message.flag) {
        "YELLOW", "DOUBLE YELLOW" -> FlagYellow; "RED" -> F1Red; "GREEN" -> DRSGreen; else -> TextSecondary
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp)) {
        Text(message.message ?: message.category ?: "", Modifier.padding(12.dp), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RaceControlMessageCard(message: RaceControlMessage) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        val flagColor = when (message.flag) {
            "YELLOW", "DOUBLE YELLOW" -> FlagYellow; "RED" -> F1Red; "GREEN" -> DRSGreen
            "CHEQUERED" -> TextPrimary; else -> TextTertiary
        }
        Box(Modifier.size(8.dp).offset(y = 4.dp).clip(CircleShape).background(flagColor))
        Spacer(Modifier.width(8.dp))
        Text(message.message ?: "", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RaceEventRow(event: RaceEvent, drivers: Map<Int, Driver>) {
    fun name(number: Int?) = number?.let { drivers[it]?.nameAcronym ?: "#$it" } ?: "?"
    val isPit = event.type == RaceEvent.Type.PIT_STOP
    val badgeColor = if (isPit) FlagYellow else DRSGreen
    val text = if (isPit) {
        buildString {
            append("${name(event.driverNumber)} no box")
            event.lapNumber?.let { append(" • volta $it") }
            event.stopDuration?.let { append(" • parado ${formatSeconds(it)} s") }
        }
    } else {
        buildString {
            append("${name(event.driverNumber)} passa ${name(event.otherDriverNumber)}")
            event.position?.let { append(" • P$it") }
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.clip(RoundedCornerShape(4.dp)).background(badgeColor.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(if (isPit) "PIT" else "ULTRAP", color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        // "2024-12-08T13:09:54.663+00:00" -> "13:09:54" (UTC)
        Text(event.date.substringAfter('T').take(8), fontSize = 11.sp, color = TextTertiary)
    }
}

/** 2.43 -> "2,4" */
private fun formatSeconds(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10},${tenths % 10}"
}

@Composable
private fun ErrorCard(error: String) {
    Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
        Text("Erro: $error", Modifier.padding(16.dp), color = F1Red)
    }
}
