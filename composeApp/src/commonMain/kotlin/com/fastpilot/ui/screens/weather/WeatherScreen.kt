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
import com.fastpilot.model.Weather
import com.fastpilot.ui.components.SessionPicker
import com.fastpilot.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WeatherScreen(viewModel: WeatherViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val weather = uiState.currentWeather

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))
        Text("Condições Climáticas", style = MaterialTheme.typography.headlineSmall,
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
                    Text("Carregando dados climáticos...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (weather != null) {
            // Header with session info
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if ((weather.rainfall ?: 0) > 0) "🌧️" else "☀️",
                        fontSize = 32.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if ((weather.rainfall ?: 0) > 0) "Condições de chuva" else "Pista seca",
                            fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp
                        )
                        Text(
                            "${uiState.selectedSession?.circuitShortName ?: ""} • ${uiState.selectedSession?.sessionName ?: ""}",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Temperature cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherGauge("Temp. Ar", "${weather.airTemperature ?: "--"}°C",
                    SpeedBlue, Modifier.weight(1f))
                WeatherGauge("Temp. Pista", "${weather.trackTemperature ?: "--"}°C",
                    F1Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherInfoCard("💧 Humidade", "${weather.humidity ?: "--"}%", Modifier.weight(1f))
                WeatherInfoCard(
                    if ((weather.rainfall ?: 0) > 0) "🌧️ Chuva" else "☀️ Seco",
                    if ((weather.rainfall ?: 0) > 0) "Sim" else "Não",
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherInfoCard("🌬️ Vento", "${weather.windSpeed ?: "--"} km/h", Modifier.weight(1f))
                WindDirection(weather.windDirection ?: 0, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            WeatherInfoCard("📊 Pressão", "${weather.pressure ?: "--"} hPa", Modifier.fillMaxWidth())

            // History stats
            if (uiState.weatherHistory.size > 1) {
                Spacer(Modifier.height(16.dp))
                Text("Histórico da sessão", color = TextSecondary, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                val temps = uiState.weatherHistory.mapNotNull { it.trackTemperature }
                val winds = uiState.weatherHistory.mapNotNull { it.windSpeed }

                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${uiState.weatherHistory.size} medições", color = TextTertiary, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        if (temps.isNotEmpty()) {
                            Row {
                                Text("Pista: ", color = TextSecondary, fontSize = 12.sp)
                                Text("min ${temps.min().toInt()}° / max ${temps.max().toInt()}°",
                                    color = F1Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (winds.isNotEmpty()) {
                            Row {
                                Text("Vento: ", color = TextSecondary, fontSize = 12.sp)
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
                    Text("🌤️", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sem dados de clima", color = TextSecondary)
                    Text("Selecione outra sessão acima", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = F1Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)) {
                Text(error, Modifier.padding(12.dp), color = F1Red, fontSize = 12.sp)
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
private fun WeatherInfoCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 13.sp, color = TextSecondary)
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
            Text("Direção do Vento", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text("↑", fontSize = 28.sp, color = DRSGreen,
                modifier = Modifier.rotate(degrees.toFloat()))
            Text("${degrees}°", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
