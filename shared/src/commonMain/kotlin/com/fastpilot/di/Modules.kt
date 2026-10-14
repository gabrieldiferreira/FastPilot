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
    single { SessionRepository(get(), getOrNull()) }
    single { DriverRepository(get(), getOrNull()) }
    single { LapRepository(get(), getOrNull()) }
    single { StintRepository(get(), getOrNull()) }
    single { TelemetryRepository(get(), getOrNull()) }
    single { PositionRepository(get()) }
    single { IntervalRepository(get()) }
    single { RaceControlRepository(get()) }
    single { WeatherRepository(get()) }
    single { TeamRadioRepository(get()) }
    single { PitStopRepository(get()) }
    single { MeetingRepository(get()) }
    single { LocationRepository(get(), getOrNull()) }
    single { OvertakeRepository(get()) }
    single { CacheMaintenance(getOrNull()) }
    single { StandingsRepository(get(), getOrNull()) }
}

val pollingModule = module {
    single { LivePollingManager(get(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { GetLiveTelemetryUseCase(get()) }
    factory { GetLivePositionsUseCase(get(), get()) }
    factory { GetDriverDuelUseCase(get()) }
    factory { CalculateLapDeltaUseCase() }
    factory { GetLapDeltaUseCase(get(), get()) }
    factory { CalculateCleanPaceUseCase() }
    factory { CalculateTheoreticalBestUseCase() }
    factory { GetTyreDegradationUseCase() }
    factory { CalculateChampionshipGapsUseCase() }
    factory { GetRaceEventsUseCase(get(), get()) }
}

val sharedModules = listOf(
    networkModule,
    repositoryModule,
    pollingModule,
    useCaseModule
)
