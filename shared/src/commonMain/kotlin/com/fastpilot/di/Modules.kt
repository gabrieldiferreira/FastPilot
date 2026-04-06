package com.fastpilot.di

import com.fastpilot.data.polling.LivePollingManager
import com.fastpilot.data.repository.*
import com.fastpilot.domain.usecase.*
import com.fastpilot.network.JolpicaApi
import com.fastpilot.network.OpenF1Api
import com.fastpilot.network.createHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
    single { OpenF1Api(get()) }
    single { JolpicaApi(get()) }
}

val repositoryModule = module {
    single { SessionRepository(get(), get()) }
    single { DriverRepository(get(), get()) }
    single { LapRepository(get(), get()) }
    single { StintRepository(get(), get()) }
    single { TelemetryRepository(get()) }
    single { PositionRepository(get()) }
    single { IntervalRepository(get()) }
    single { RaceControlRepository(get()) }
    single { WeatherRepository(get()) }
    single { TeamRadioRepository(get()) }
    single { PitStopRepository(get()) }
    single { MeetingRepository(get()) }
}

val pollingModule = module {
    single { LivePollingManager(get(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { GetLiveTelemetryUseCase(get()) }
    factory { GetLivePositionsUseCase(get(), get()) }
    factory { GetDriverDuelUseCase(get()) }
    factory { CalculateCleanPaceUseCase() }
    factory { CalculateTheoreticalBestUseCase() }
    factory { GetTyreDegradationUseCase() }
}

val sharedModules = listOf(
    networkModule,
    repositoryModule,
    pollingModule,
    useCaseModule
)
