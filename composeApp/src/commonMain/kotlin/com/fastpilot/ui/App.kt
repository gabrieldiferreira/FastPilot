package com.fastpilot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.fastpilot.data.repository.CacheMaintenance
import com.fastpilot.ui.i18n.LanguageState
import com.fastpilot.ui.i18n.LocalStrings
import com.fastpilot.ui.navigation.*
import com.fastpilot.ui.screens.splash.SplashScreen
import com.fastpilot.ui.theme.*
import org.koin.compose.koinInject

enum class AppState { SPLASH, LOGIN, DASHBOARD }

/**
 * [benchmark] is set when the app is launched in benchmark mode: `platform` and the
 * [com.fastpilot.ui.perf.LoadBenchmark.Config] spec ("off:5,on:3,...").
 */
@Composable
fun App(benchmark: Pair<String, String>? = null) {
    var appState by remember { mutableStateOf(AppState.SPLASH) }

    if (benchmark != null) {
        FastPilotTheme { com.fastpilot.ui.perf.BenchmarkScreen(benchmark.first, benchmark.second) }
        return
    }

    // Every screen reads its texts from LocalStrings; picking another language recomposes them
    CompositionLocalProvider(LocalStrings provides LanguageState.current.strings) {
        AppContent(appState) { appState = it }
    }
}

@Composable
private fun AppContent(appState: AppState, onStateChange: (AppState) -> Unit) {
    // Deletes race data downloaded more than 30 days ago (not in benchmark mode: it times the cache)
    val cacheMaintenance = koinInject<CacheMaintenance>()
    LaunchedEffect(Unit) { cacheMaintenance.purgeExpired() }

    FastPilotTheme {
        Crossfade(targetState = appState) { state ->
            when (state) {
                AppState.SPLASH -> {
                    SplashScreen(onSplashFinished = { onStateChange(AppState.LOGIN) })
                }
                AppState.LOGIN -> {
                    com.fastpilot.ui.screens.login.LoginScreen(onLoginSuccess = { onStateChange(AppState.DASHBOARD) })
                }
                AppState.DASHBOARD -> {
                    TabNavigator(DashboardTab) { tabNavigator ->
                        Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = CarbonBlack,
                        bottomBar = {
                            FastPilotBottomBar(tabNavigator)
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(
                                    Brush.verticalGradient(DarkGradientColors)
                                )
                        ) {
                            CurrentTab()
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastPilotBottomBar(tabNavigator: TabNavigator) {
    val tabs = listOf(
        DashboardTab,
        TelemetryTab,
        StrategyTab,
        DuelTab,
        RadioTab,
        WeatherTab,
        StandingsTab
    )

    NavigationBar(
        containerColor = NightSurface,
        contentColor = TextSecondary,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = tabNavigator.current.key == tab.key
            NavigationBarItem(
                selected = selected,
                onClick = { tabNavigator.current = tab },
                icon = {
                    tab.options.icon?.let { painter ->
                        Icon(
                            painter = painter,
                            contentDescription = tab.options.title,
                            tint = if (selected) F1Red else TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    androidx.compose.material3.Text(
                        text = tab.options.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) TextPrimary else TextTertiary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = false, // 7 tabs: labels only fit on the selected one
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = F1Red.copy(alpha = 0.15f)
                )
            )
        }
    }
}
