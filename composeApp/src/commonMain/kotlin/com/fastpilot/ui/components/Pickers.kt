package com.fastpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.model.Session
import com.fastpilot.ui.theme.*

/**
 * Reusable session picker with dropdown menus for year and session selection.
 */
@Composable
fun SessionPicker(
    sessions: List<Session>,
    selectedSession: Session?,
    onSessionSelected: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    var yearExpanded by remember { mutableStateOf(false) }
    var sessionExpanded by remember { mutableStateOf(false) }

    // Group sessions by year
    val years = sessions.mapNotNull { it.year }.distinct().sortedDescending()
    val selectedYear = selectedSession?.year ?: years.firstOrNull() ?: 2024

    // Group by circuit (meeting) within selected year
    val yearSessions = sessions.filter { it.year == selectedYear }
    val circuits = yearSessions
        .map { it.circuitShortName ?: it.countryName ?: "Unknown" }
        .distinct()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Year selector
            Box(modifier = Modifier.weight(0.35f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { yearExpanded = true },
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🏁 $selectedYear",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Selecionar ano",
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
                                // Select first session of this year
                                sessions.firstOrNull { it.year == year }?.let { onSessionSelected(it) }
                            }
                        )
                    }
                }
            }

            // Session selector
            Box(modifier = Modifier.weight(0.65f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sessionExpanded = true },
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedSession?.circuitShortName ?: "Selecione",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                selectedSession?.sessionName ?: "",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Selecionar sessão",
                            tint = TextTertiary, modifier = Modifier.size(20.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = sessionExpanded,
                    onDismissRequest = { sessionExpanded = false },
                    modifier = Modifier.background(DarkSurface).heightIn(max = 400.dp)
                ) {
                    yearSessions.forEach { session ->
                        val isSelected = session.sessionKey == selectedSession?.sessionKey
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "${session.circuitShortName ?: session.countryName ?: ""}",
                                        color = if (isSelected) F1Red else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        "${session.sessionName ?: ""} • ${session.countryName ?: ""}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                sessionExpanded = false
                                onSessionSelected(session)
                            }
                        )
                    }
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
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
                        } ?: "Selecione um piloto",
                        fontSize = 13.sp,
                        color = if (selectedDriver != null) TextPrimary else TextTertiary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Selecionar piloto",
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
