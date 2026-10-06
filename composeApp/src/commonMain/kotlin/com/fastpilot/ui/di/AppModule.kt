package com.fastpilot.ui.di

import com.fastpilot.di.sharedModules
import com.fastpilot.ui.screens.dashboard.DashboardViewModel
import com.fastpilot.ui.screens.duel.DuelViewModel
import com.fastpilot.ui.screens.radio.RadioViewModel
import com.fastpilot.ui.screens.strategy.StrategyViewModel
import com.fastpilot.ui.screens.telemetry.TelemetryViewModel
import com.fastpilot.ui.screens.weather.WeatherViewModel
import com.fastpilot.ui.screens.standings.StandingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }
    viewModel { TelemetryViewModel(get(), get(), get(), get()) }
    viewModel { StrategyViewModel(get(), get(), get(), get()) }
    viewModel { DuelViewModel(get(), get(), get(), get()) }
    viewModel { RadioViewModel(get(), get(), get()) }
    viewModel { WeatherViewModel(get(), get()) }
    viewModel { StandingsViewModel(get(), get()) }
}

val appModules = sharedModules + viewModelModule
