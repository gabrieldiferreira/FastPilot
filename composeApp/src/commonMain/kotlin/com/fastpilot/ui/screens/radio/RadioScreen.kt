package com.fastpilot.ui.screens.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.model.TeamRadio
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import com.fastpilot.media.AudioPlayer
import com.fastpilot.media.createAudioPlayer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RadioScreen(viewModel: RadioViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val audioPlayer = remember { createAudioPlayer() }
    var currentPlayingUrl by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Rádio de Equipe", style = MaterialTheme.typography.headlineSmall,
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

        // Driver filter chips
        if (uiState.drivers.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = uiState.selectedDriver == null,
                        onClick = { viewModel.filterByDriver(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = F1Red.copy(alpha = 0.3f), containerColor = CardSurface))
                }
                items(uiState.drivers) { driver ->
                    val sel = uiState.selectedDriver?.driverNumber == driver.driverNumber
                    val tc = driver.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary
                    FilterChip(selected = sel, onClick = { viewModel.filterByDriver(driver) },
                        label = { Text(driver.nameAcronym ?: "#${driver.driverNumber}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.copy(alpha = 0.3f), containerColor = CardSurface))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        val filtered = uiState.selectedDriver?.let { d ->
            uiState.radios.filter { it.driverNumber == d.driverNumber }
        } ?: uiState.radios

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text("Carregando rádios...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📻", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma comunicação de rádio", color = TextSecondary)
                    Text("encontrada para esta sessão", color = TextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            // Radio count
            Text("${filtered.size} comunicações", color = TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered.reversed()) { radio ->
                    RadioCard(
                        radio = radio, 
                        driver = uiState.drivers.find { it.driverNumber == radio.driverNumber },
                        isPlaying = currentPlayingUrl == radio.recordingUrl,
                        onPlayClicked = { url ->
                            if (currentPlayingUrl == url) {
                                audioPlayer.pause()
                                currentPlayingUrl = null
                            } else {
                                audioPlayer.play(url)
                                currentPlayingUrl = url
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        uiState.error?.let { error ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(error, Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RadioCard(
    radio: TeamRadio, 
    driver: com.fastpilot.model.Driver?,
    isPlaying: Boolean,
    onPlayClicked: (String) -> Unit
) {
    val tc = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Team color bar
                Box(Modifier.width(4.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(tc))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(driver?.nameAcronym ?: "#${radio.driverNumber}",
                            fontWeight = FontWeight.Bold, color = tc, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(driver?.teamName ?: "", fontSize = 11.sp, color = TextTertiary)
                    }
                    // Timestamp
                    val timestamp = radio.date?.let { parseTimestamp(it) } ?: ""
                    if (timestamp.isNotEmpty()) {
                        Text("🕐 $timestamp", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Audio Playback
            radio.recordingUrl?.let { url ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onPlayClicked(url) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) F1Red.copy(alpha = 0.15f) else DRSGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶", 
                            color = if (isPlaying) F1Red else DRSGreen, 
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "Reproduzindo nativamente..." else "Ouvir comunicação no App",
                                color = if (isPlaying) F1Red else DRSGreen, 
                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                            Text(url, color = TextTertiary, fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            } ?: run {
                Text("⚠️ URL do áudio não disponível", color = TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

private fun parseTimestamp(dateStr: String): String {
    // Format: "2024-03-24T00:15:30.123+00:00" -> "00:15:30"
    return try {
        val tIndex = dateStr.indexOf('T')
        if (tIndex >= 0) {
            val timeStr = dateStr.substring(tIndex + 1)
            timeStr.take(8) // "HH:mm:ss"
        } else dateStr
    } catch (_: Exception) { dateStr }
}
