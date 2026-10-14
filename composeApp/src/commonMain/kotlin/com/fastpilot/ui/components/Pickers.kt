package com.fastpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Flag
import com.fastpilot.data.repository.availableSeasons
import com.fastpilot.model.Session
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.theme.*

/**
 * Reusable session picker: year and race weekend dropdowns, then one chip per event of the weekend.
 */
@Composable
fun SessionPicker(
    sessions: List<Session>,
    selectedSession: Session?,
    onSessionSelected: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    var yearExpanded by remember { mutableStateOf(false) }
    var weekendExpanded by remember { mutableStateOf(false) }

    val years = sessions.mapNotNull { it.year }.distinct().sortedDescending()
    val selectedYear = selectedSession?.year ?: years.firstOrNull() ?: availableSeasons().first()

    val weekends = remember(sessions, selectedYear) {
        groupRaceWeekends(sessions.filter { it.year == selectedYear })
    }
    val selectedWeekend = weekends.firstOrNull { w -> w.sessions.any { it.sessionKey == selectedSession?.sessionKey } }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Year selector
            Box(modifier = Modifier.weight(0.32f)) {
                Card(
                    onClick = { yearExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$selectedYear",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = strings.selectYear,
                            tint = TextTertiary, modifier = Modifier.size(20.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text("$year", color = if (year == selectedYear) F1Red else TextPrimary)
                            },
                            onClick = {
                                yearExpanded = false
                                // Most recent weekend of that year, keeping the event when it has one
                                groupRaceWeekends(sessions.filter { it.year == year }).firstOrNull()
                                    ?.let { onSessionSelected(pickSessionInWeekend(it, selectedSession)) }
                            }
                        )
                    }
                }
            }

            // Race weekend selector
            Box(modifier = Modifier.weight(0.68f)) {
                Card(
                    onClick = { weekendExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedWeekend?.title(strings) ?: strings.select,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                selectedWeekend?.subtitle(strings) ?: "",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = strings.selectRaceWeekend,
                            tint = TextTertiary, modifier = Modifier.size(20.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = weekendExpanded,
                    onDismissRequest = { weekendExpanded = false },
                    modifier = Modifier.background(DarkSurface).heightIn(max = 400.dp)
                ) {
                    weekends.forEach { weekend ->
                        val isSelected = weekend.key == selectedWeekend?.key
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        weekend.title(strings),
                                        color = if (isSelected) F1Red else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(weekend.subtitle(strings), color = TextSecondary, fontSize = 11.sp)
                                }
                            },
                            onClick = {
                                weekendExpanded = false
                                onSessionSelected(pickSessionInWeekend(weekend, selectedSession))
                            }
                        )
                    }
                }
            }
        }

        // Events of the selected weekend
        selectedWeekend?.let { weekend ->
            Spacer(Modifier.height(8.dp))
            val chipsState = rememberLazyListState()
            val selectedIndex = weekend.sessions.indexOfFirst { it.sessionKey == selectedSession?.sessionKey }
            LaunchedEffect(weekend.key, selectedIndex) {
                if (selectedIndex >= 0) chipsState.animateScrollToItem(selectedIndex)
            }
            LazyRow(state = chipsState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weekend.sessions, key = { it.sessionKey }) { session ->
                    val sel = session.sessionKey == selectedSession?.sessionKey
                    FilterChip(
                        selected = sel,
                        onClick = { if (!sel) onSessionSelected(session) },
                        label = { Text(strings.sessionShort(session.sessionName), fontSize = 12.sp) },
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
    }
}

/**
 * Driver selector dropdown.
 */
@Composable
fun DriverSelector(
    label: String,
    drivers: List<com.fastpilot.model.Driver>,
    selectedDriver: com.fastpilot.model.Driver?,
    onDriverSelected: (com.fastpilot.model.Driver) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val teamColor = selectedDriver?.teamColour?.let { TeamColors.fromHex(it) } ?: TextTertiary

    Box(modifier = modifier) {
        Card(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(teamColor)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                    Text(
                        selectedDriver?.let {
                            "${it.nameAcronym ?: ""} — ${it.fullName ?: ""}"
                        } ?: strings.selectADriver,
                        fontSize = 13.sp,
                        color = if (selectedDriver != null) TextPrimary else TextTertiary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = strings.selectDriver,
                    tint = TextTertiary, modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface).heightIn(max = 400.dp)
        ) {
            drivers.forEach { driver ->
                val driverTeamColor = driver.teamColour?.let { TeamColors.fromHex(it) } ?: TextTertiary
                val isSelected = driver.driverNumber == selectedDriver?.driverNumber
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(driverTeamColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "#${driver.driverNumber}",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                modifier = Modifier.width(30.dp)
                            )
                            Text(
                                "${driver.nameAcronym ?: ""}",
                                color = if (isSelected) F1Red else driverTeamColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                driver.teamName ?: "",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onDriverSelected(driver)
                    }
                )
            }
        }
    }
}
