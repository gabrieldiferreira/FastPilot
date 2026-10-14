package com.fastpilot.ui.screens.weather

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.fastpilot.model.Weather
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WeatherScreen(viewModel: WeatherViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val weather = uiState.currentWeather

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))
        Text(strings.weatherTitle, style = MaterialTheme.typography.headlineSmall,
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

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = F1Red)
                    Spacer(Modifier.height(8.dp))
                    Text(strings.loadingWeather, color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (weather != null) {
            // Header with session info
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if ((weather.rainfall ?: 0) > 0) Icons.Default.Umbrella else Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if ((weather.rainfall ?: 0) > 0) SpeedBlue else FlagYellow,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if ((weather.rainfall ?: 0) > 0) strings.wetConditions else strings.dryTrack,
                            fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp
                        )
                        Text(
                            "${strings.place(uiState.selectedSession?.circuitShortName)} • ${strings.sessionLong(uiState.selectedSession?.sessionName)}",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Temperature cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherGauge(strings.airTemp, "${weather.airTemperature ?: "--"}°C",
                    SpeedBlue, Modifier.weight(1f))
                WeatherGauge(strings.trackTemp, "${weather.trackTemperature ?: "--"}°C",
                    F1Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherInfoCard(Icons.Default.WaterDrop, strings.humidity, "${weather.humidity ?: "--"}%", Modifier.weight(1f))
                WeatherInfoCard(
                    Icons.Default.Umbrella, strings.rain,
                    if ((weather.rainfall ?: 0) > 0) strings.yes else strings.no,
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherInfoCard(Icons.Default.Air, strings.wind, "${weather.windSpeed ?: "--"} km/h", Modifier.weight(1f))
                WindDirection(weather.windDirection ?: 0, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            WeatherInfoCard(Icons.Default.Compress, strings.pressure, "${weather.pressure ?: "--"} hPa", Modifier.fillMaxWidth())

            // History stats
            if (uiState.weatherHistory.size > 1) {
                Spacer(Modifier.height(16.dp))
                Text(strings.sessionHistory, color = TextSecondary, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                val temps = uiState.weatherHistory.mapNotNull { it.trackTemperature }
                val winds = uiState.weatherHistory.mapNotNull { it.windSpeed }

                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(strings.readings(uiState.weatherHistory.size), color = TextTertiary, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        if (temps.isNotEmpty()) {
                            Row {
                                Text(strings.trackPrefix, color = TextSecondary, fontSize = 12.sp)
                                Text("min ${temps.min().toInt()}° / max ${temps.max().toInt()}°",
                                    color = F1Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (winds.isNotEmpty()) {
                            Row {
                                Text(strings.windPrefix, color = TextSecondary, fontSize = 12.sp)
                                Text("min ${winds.min().toInt()} / max ${winds.max().toInt()} km/h",
                                    color = SpeedBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(strings.noWeather, color = TextSecondary)
                    Text(strings.selectAnotherSession, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.error(error), Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun WeatherGauge(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun WeatherInfoCard(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 13.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun WindDirection(degrees: Int, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(strings.windDirection, fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = DRSGreen,
                modifier = Modifier.size(28.dp).rotate(degrees.toFloat()))
            Text("${degrees}°", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
