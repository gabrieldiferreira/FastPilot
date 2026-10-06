package com.fastpilot.ui.screens.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.domain.usecase.ChampionshipRow
import com.fastpilot.domain.usecase.ChampionshipTable
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun StandingsScreen(viewModel: StandingsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val tables = uiState.tables

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Classificação", style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    if (tables != null && tables.round > 0) "Temporada ${tables.season} • após a etapa ${tables.round}"
                    else "Temporada ${uiState.selectedSeason}",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            IconButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Season + mode selectors ─────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            uiState.seasons.forEach { season ->
                Pill(season.toString(), selected = season == uiState.selectedSeason) {
                    viewModel.selectSeason(season)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardSurface).padding(4.dp)
        ) {
            ModeTab("Pilotos", uiState.mode == StandingsMode.DRIVERS, Modifier.weight(1f)) {
                viewModel.selectMode(StandingsMode.DRIVERS)
            }
            ModeTab("Equipes", uiState.mode == StandingsMode.CONSTRUCTORS, Modifier.weight(1f)) {
                viewModel.selectMode(StandingsMode.CONSTRUCTORS)
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            uiState.isLoading -> CenteredMessage {
                CircularProgressIndicator(color = F1Red)
                Spacer(Modifier.height(8.dp))
                Text("Carregando classificação...", color = TextSecondary, fontSize = 12.sp)
            }
            uiState.error != null -> CenteredMessage {
                Text(uiState.error!!, color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = viewModel::refresh) { Text("Tentar novamente", color = F1Red) }
            }
            tables == null || tables.drivers.rows.isEmpty() -> CenteredMessage {
                Text("Sem classificação para esta temporada", color = TextSecondary)
            }
            uiState.mode == StandingsMode.DRIVERS -> StandingsList(tables.drivers) { row ->
                StandingRow(
                    row = row,
                    leaderPoints = tables.drivers.leaderPoints,
                    title = row.entry.fullName,
                    subtitle = row.entry.constructorName,
                    teamColor = TeamColors.fromConstructorId(row.entry.constructorId),
                    wins = row.entry.wins
                )
            }
            else -> StandingsList(tables.constructors) { row ->
                StandingRow(
                    row = row,
                    leaderPoints = tables.constructors.leaderPoints,
                    title = row.entry.name,
                    subtitle = null,
                    teamColor = TeamColors.fromConstructorId(row.entry.constructorId),
                    wins = row.entry.wins
                )
            }
        }
    }
}

@Composable
private fun <T> StandingsList(table: ChampionshipTable<T>, rowContent: @Composable (ChampionshipRow<T>) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(table.rows, key = { it.position }) { rowContent(it) }
    }
}

@Composable
private fun StandingRow(
    row: ChampionshipRow<*>,
    leaderPoints: Double,
    title: String,
    subtitle: String?,
    teamColor: Color,
    wins: Int
) {
    val isLeader = row.position == 1
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isLeader) CardSurfaceHover else CardSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.position.toString(),
                color = if (isLeader) FlagYellow else TextPrimary,
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.width(28.dp)
            )
            Box(Modifier.width(4.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (wins > 0) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Vitórias",
                            tint = FlagYellow, modifier = Modifier.size(12.dp))
                        Text(wins.toString(), color = FlagYellow, fontSize = 11.sp)
                    }
                }
                if (subtitle != null) {
                    Text(subtitle, color = TextTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (leaderPoints > 0) (row.points / leaderPoints).toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = teamColor,
                    trackColor = DividerColor,
                    drawStopIndicator = {}
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 56.dp)) {
                Text(formatPoints(row.points), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    if (isLeader) "líder" else "-${formatPoints(row.gapToLeader)}",
                    color = if (isLeader) FlagYellow else TextTertiary, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) F1Red else CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MidSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) TextPrimary else TextTertiary, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CenteredMessage(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

/** 437.0 -> "437", 12.5 -> "12.5" (sprint/half points). */
private fun formatPoints(points: Double): String =
    if (points % 1.0 == 0.0) points.toInt().toString()
    else ((points * 10).roundToInt() / 10.0).toString()
