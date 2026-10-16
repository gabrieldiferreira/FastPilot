package com.fastpilot.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.fastpilot.domain.usecase.bestLapsByDriver
import com.fastpilot.domain.usecase.deletedLaps
import com.fastpilot.domain.usecase.lapsByPhase
import com.fastpilot.domain.usecase.qualifyingPhases
import com.fastpilot.model.Driver
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.i18n.oneDecimal
import com.fastpilot.ui.components.AccountButton
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.time.localClock
import com.fastpilot.ui.time.localizeTrackTimes
import com.fastpilot.ui.time.userOffsetLabel
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
            sessionName = uiState.currentSession?.let { strings.sessionLong(it.sessionName) } ?: strings.loading,
            circuitName = strings.place(uiState.currentSession?.circuitShortName),
            countryName = strings.place(uiState.currentSession?.countryName),
            isLive = uiState.isLive
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text(strings.loadingData, color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (uiState.error != null) {
            ErrorCard(strings.error(uiState.error!!))
        } else if (uiState.positions.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(strings.noPositions, color = TextSecondary)
                    Text(strings.selectAnotherSession, color = TextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            // Practice and qualifying: best laps, without the laps deleted by race control,
            // split into Q1/Q2/Q3 when race control marks the phases
            val session = uiState.currentSession
            val drivers = uiState.positions.associate { it.position.driverNumber to it.driver }
            val deleted = remember(uiState.raceControlMessages) { deletedLaps(uiState.raceControlMessages) }
            val phases = remember(session, uiState.raceControlMessages) {
                if (session?.sessionType == "Qualifying") qualifyingPhases(uiState.raceControlMessages) else emptyList()
            }
            val phaseNumbers = phases.map { it.number }
            val isSprint = session?.sessionName?.startsWith("Sprint") == true
            var selectedPhase by remember(session?.sessionKey) { mutableStateOf<Int?>(null) }
            val phase = selectedPhase?.takeIf { it in phaseNumbers } ?: phaseNumbers.lastOrNull()
            val timingRows = remember(uiState.positions, uiState.laps, deleted, phases, phase) {
                timingRows(uiState.positions, uiState.laps, deleted, phases, phase)
            }
            val fastestLap = timingRows.mapNotNull { it.bestLap }.minOrNull()

            // Above the list, not in it: race control arrives after the positions, and an item
            // inserted at the top of a LazyColumn ends up scrolled out of view
            RaceControlBanner(uiState.raceControlMessages.maxByOrNull { it.date.orEmpty() }, session?.gmtOffset)
            if (phaseNumbers.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                QualifyingPhaseChips(phaseNumbers, phase, isSprint) { selectedPhase = it }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                // Position cards
                if (uiState.currentSession?.isRace() == true) {
                    items(uiState.positions) { posWithDriver ->
                        PositionCard(
                            posWithDriver.position.position, posWithDriver.position.driverNumber, posWithDriver.driver,
                            uiState.intervals[posWithDriver.position.driverNumber]
                                ?.let { strings.gap(it.intervalDisplay) to "Gap: ${strings.gap(it.gapToLeaderDisplay)}" }
                                ?: ("--" to "Gap: --")
                        )
                    }
                } else {
                    items(timingRows) { row ->
                        PositionCard(row.rank, row.driverNumber, drivers[row.driverNumber], row.bestLap?.let { best ->
                            formatLapTime(best) to
                                if (best == fastestLap) strings.fastest else "+${formatGap(best - fastestLap!!)}"
                        })
                    }
                }

                // Every time below is in the user's time zone
                if (uiState.events.isNotEmpty() || uiState.raceControlMessages.isNotEmpty()) {
                    item {
                        Text(strings.localTimes(userOffsetLabel()), fontSize = 11.sp, color = TextTertiary,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }

                // Pit stops and overtakes
                if (uiState.events.isNotEmpty()) {
                    val drivers = uiState.positions.mapNotNull { it.driver }.associateBy { it.driverNumber }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(strings.events, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.events) { event -> RaceEventRow(event, drivers) }
                }

                // Race Control Feed
                if (uiState.raceControlMessages.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(strings.raceControl, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.raceControlMessages.sortedByDescending { it.date.orEmpty() }.take(10)) { msg ->
                        RaceControlMessageCard(msg, session?.gmtOffset)
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
    val labelText = if (isLive) strings.live else strings.historical
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
            AccountButton()
        }
    }
}

@Composable
/** [timing]: main value and the line under it (interval and gap in races, best lap and gap to the fastest otherwise). */
private fun PositionCard(rank: Int, driverNumber: Int, driver: Driver?, timing: Pair<String, String>?) {
    val teamColor = driver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(
                    when (rank) { 1 -> F1Red; 2, 3 -> DarkSurface; else -> CardSurfaceHover }
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("$rank", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver?.nameAcronym ?: "#$driverNumber", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(driver?.teamName ?: "", fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(timing?.first ?: "--", fontSize = 14.sp, color = DRSGreen, fontWeight = FontWeight.Medium)
                Text(timing?.second ?: strings.noTime, fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun RaceControlBanner(message: RaceControlMessage?, trackGmtOffset: String?) {
    if (message == null) return
    val bgColor = when (message.flag) {
        "YELLOW", "DOUBLE YELLOW" -> FlagYellow.copy(alpha = 0.2f); "RED" -> F1Red.copy(alpha = 0.2f)
        "GREEN" -> DRSGreen.copy(alpha = 0.2f); else -> CardSurface
    }
    val textColor = when (message.flag) {
        "YELLOW", "DOUBLE YELLOW" -> FlagYellow; "RED" -> F1Red; "GREEN" -> DRSGreen; else -> TextSecondary
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp)) {
        Text(message.message?.let { raceControlText(it, message.date, trackGmtOffset) } ?: message.category ?: "", Modifier.padding(12.dp), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RaceControlMessageCard(message: RaceControlMessage, trackGmtOffset: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        val flagColor = when (message.flag) {
            "YELLOW", "DOUBLE YELLOW" -> FlagYellow; "RED" -> F1Red; "GREEN" -> DRSGreen
            "CHEQUERED" -> TextPrimary; else -> TextTertiary
        }
        Box(Modifier.size(8.dp).offset(y = 4.dp).clip(CircleShape).background(flagColor))
        Spacer(Modifier.width(8.dp))
        Text(message.message?.let { raceControlText(it, message.date, trackGmtOffset) } ?: "", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(localClock(message.date) ?: "", fontSize = 11.sp, color = TextTertiary)
            message.lapNumber?.let { Text(strings.lap(it), fontSize = 10.sp, color = TextTertiary) }
        }
    }
}

@Composable
private fun RaceEventRow(event: RaceEvent, drivers: Map<Int, Driver>) {
    fun name(number: Int?) = number?.let { drivers[it]?.nameAcronym ?: "#$it" } ?: "?"
    val isPit = event.type == RaceEvent.Type.PIT_STOP
    val badgeColor = if (isPit) FlagYellow else DRSGreen
    val s = strings
    val text = if (isPit) {
        buildString {
            append(s.pitStop(name(event.driverNumber)))
            // OpenF1 stopped sending the stationary time in 2026; the pit lane time is always there
            event.stopDuration?.let { append(" • " + s.stationary(oneDecimal(it, s.decimalSeparator))) }
                ?: event.laneDuration?.let { append(" • " + s.pitLane(oneDecimal(it, s.decimalSeparator))) }
        }
    } else {
        buildString {
            append(s.overtake(name(event.driverNumber), name(event.otherDriverNumber)))
            event.position?.let { append(" • P$it") }
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.clip(RoundedCornerShape(4.dp)).background(badgeColor.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(if (isPit) "PIT" else s.overtakeBadge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(localClock(event.date) ?: "", fontSize = 11.sp, color = TextTertiary)
            event.lapNumber?.let { Text(s.lap(it), fontSize = 10.sp, color = TextTertiary) }
        }
    }
}

/** A race control message in the app language, with the circuit times in the user's time zone. */
@Composable
private fun raceControlText(message: String, sentAt: String?, trackGmtOffset: String?) =
    localizeTrackTimes(strings.raceControl(message), sentAt, trackGmtOffset)

/** 95.123 -> "1:35.123" */
private fun formatLapTime(seconds: Double): String {
    val millis = (seconds * 1000).roundToInt()
    val secs = (millis / 1000) % 60
    return "${millis / 60000}:${secs.toString().padStart(2, '0')}.${(millis % 1000).toString().padStart(3, '0')}"
}

/** 0.3456 -> "0.346" */
private fun formatGap(seconds: Double): String {
    val millis = (seconds * 1000).roundToInt()
    return "${millis / 1000}.${(millis % 1000).toString().padStart(3, '0')}"
}

@Composable
private fun ErrorCard(error: String) {
    Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
        Text(error, Modifier.padding(16.dp), color = F1Red)
    }
}

/** One line of the practice/qualifying timing: [rank] by best lap, drivers without a time last. */
private data class TimingRow(val rank: Int, val driverNumber: Int, val bestLap: Double?)

/**
 * Drivers ordered by best lap. With a qualifying [phase], only the laps of that phase and the
 * drivers who went out in it; otherwise the whole session and every driver in [positions].
 */
private fun timingRows(
    positions: List<PositionWithDriver>,
    laps: List<com.fastpilot.model.Lap>,
    deleted: Set<Pair<Int, Int>>,
    phases: List<com.fastpilot.domain.usecase.QualifyingPhase>,
    phase: Int?
): List<TimingRow> {
    val phaseLaps = if (phase != null) lapsByPhase(laps, phases)[phase].orEmpty() else laps
    val best = bestLapsByDriver(phaseLaps, deleted)
    val drivers = if (phase != null) phaseLaps.map { it.driverNumber }.distinct()
        else positions.map { it.position.driverNumber }
    // Ties and drivers without a time keep the official order
    val official = positions.withIndex().associate { (i, p) -> p.position.driverNumber to i }
    return drivers.sortedWith(compareBy({ best[it] ?: Double.MAX_VALUE }, { official[it] ?: Int.MAX_VALUE }))
        .mapIndexed { i, driver -> TimingRow(i + 1, driver, best[driver]) }
}

@Composable
private fun QualifyingPhaseChips(phases: List<Int>, selected: Int?, isSprint: Boolean, onSelect: (Int) -> Unit) {
    if (phases.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(phases) { phase ->
            FilterChip(
                selected = phase == selected,
                onClick = { onSelect(phase) },
                label = { Text(if (isSprint) "SQ$phase" else "Q$phase", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = F1Red.copy(alpha = 0.3f),
                    selectedLabelColor = TextPrimary,
                    containerColor = CardSurface,
                    labelColor = TextSecondary
                )
            )
        }
    }
}
