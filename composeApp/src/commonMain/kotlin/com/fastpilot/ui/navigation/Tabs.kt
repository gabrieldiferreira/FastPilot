package com.fastpilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.screens.dashboard.DashboardScreen
import com.fastpilot.ui.screens.telemetry.TelemetryScreen
import com.fastpilot.ui.screens.strategy.StrategyScreen
import com.fastpilot.ui.screens.duel.DuelScreen
import com.fastpilot.ui.screens.radio.RadioScreen
import com.fastpilot.ui.screens.weather.WeatherScreen
import com.fastpilot.ui.screens.standings.StandingsScreen

object DashboardTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Speed)
            val title = strings.tabDashboard
            return remember(title) { TabOptions(index = 0u, title = title, icon = icon) }
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
            val title = strings.tabTelemetry
            return remember(title) { TabOptions(index = 1u, title = title, icon = icon) }
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
            val title = strings.tabStrategy
            return remember(title) { TabOptions(index = 2u, title = title, icon = icon) }
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
            val title = strings.tabDuel
            return remember(title) { TabOptions(index = 3u, title = title, icon = icon) }
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
            val title = strings.tabRadio
            return remember(title) { TabOptions(index = 4u, title = title, icon = icon) }
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
            val title = strings.tabWeather
            return remember(title) { TabOptions(index = 5u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        WeatherScreen()
    }
}

object StandingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Leaderboard)
            val title = strings.tabStandings
            return remember(title) { TabOptions(index = 6u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        StandingsScreen()
    }
}
