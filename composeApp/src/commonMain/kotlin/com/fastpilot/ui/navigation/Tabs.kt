package com.fastpilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.fastpilot.ui.screens.dashboard.DashboardScreen
import com.fastpilot.ui.screens.telemetry.TelemetryScreen
import com.fastpilot.ui.screens.strategy.StrategyScreen
import com.fastpilot.ui.screens.duel.DuelScreen
import com.fastpilot.ui.screens.radio.RadioScreen
import com.fastpilot.ui.screens.weather.WeatherScreen

object DashboardTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Speed)
            return remember { TabOptions(index = 0u, title = "Dashboard", icon = icon) }
        }

    @Composable
    override fun Content() {
        DashboardScreen()
    }
}

object TelemetryTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.ShowChart)
            return remember { TabOptions(index = 1u, title = "Telemetria", icon = icon) }
        }

    @Composable
    override fun Content() {
        TelemetryScreen()
    }
}

object StrategyTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Timeline)
            return remember { TabOptions(index = 2u, title = "Estratégia", icon = icon) }
        }

    @Composable
    override fun Content() {
        StrategyScreen()
    }
}

object DuelTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.AutoMirrored.Filled.CompareArrows)
            return remember { TabOptions(index = 3u, title = "Duelo", icon = icon) }
        }

    @Composable
    override fun Content() {
        DuelScreen()
    }
}

object RadioTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Radio)
            return remember { TabOptions(index = 4u, title = "Rádio", icon = icon) }
        }

    @Composable
    override fun Content() {
        RadioScreen()
    }
}

object WeatherTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Cloud)
            return remember { TabOptions(index = 5u, title = "Clima", icon = icon) }
        }

    @Composable
    override fun Content() {
        WeatherScreen()
    }
}
