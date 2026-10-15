package com.fastpilot.ui.screens.radio

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import com.fastpilot.model.TeamRadio
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.time.localClock
import com.fastpilot.ui.theme.*
import com.fastpilot.media.RadioPlayback
import com.fastpilot.media.createAudioPlayer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RadioScreen(viewModel: RadioViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // Follows the player, so a recording that ends or cannot be played stops showing as playing
    val playback = remember { RadioPlayback(createAudioPlayer()) }
    DisposableEffect(playback) { onDispose { playback.release() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text(strings.radioTitle, style = MaterialTheme.typography.headlineSmall,
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
                        label = { Text(strings.all) },
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
                    Text(strings.loadingRadios, color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(strings.noRadios, color = TextSecondary)
                    Text(strings.noRadiosForSession, color = TextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            // Radio count
            Text(strings.radioCount(filtered.size), color = TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered.reversed()) { radio ->
                    RadioCard(
                        radio = radio, 
                        driver = uiState.drivers.find { it.driverNumber == radio.driverNumber },
                        isPlaying = playback.playingUrl == radio.recordingUrl,
                        failed = playback.failedUrl == radio.recordingUrl,
                        onPlayClicked = { url -> playback.toggle(url) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        uiState.error?.let { error ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.error(error), Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RadioCard(
    radio: TeamRadio, 
    driver: com.fastpilot.model.Driver?,
    isPlaying: Boolean,
    failed: Boolean,
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
                    val timestamp = localClock(radio.date) ?: ""
                    if (timestamp.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(timestamp, fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Audio Playback
            radio.recordingUrl?.let { url ->
                Card(
                    onClick = { onPlayClicked(url) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) F1Red.copy(alpha = 0.15f) else DRSGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (isPlaying) strings.pause else strings.listen,
                            tint = if (isPlaying) F1Red else DRSGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) strings.playing else strings.listenInApp,
                                color = if (isPlaying) F1Red else DRSGreen, 
                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                            Text(url, color = TextTertiary, fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (failed) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SafetyCarAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.audioFailed, color = SafetyCarAmber, fontSize = 12.sp)
                    }
                }
            } ?: run {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.audioUnavailable, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}
