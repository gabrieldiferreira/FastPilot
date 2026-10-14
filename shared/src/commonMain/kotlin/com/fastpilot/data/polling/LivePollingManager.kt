package com.fastpilot.data.polling

import com.fastpilot.data.repository.IntervalRepository
import com.fastpilot.data.repository.PositionRepository
import com.fastpilot.data.repository.RaceControlRepository
import com.fastpilot.data.repository.WeatherRepository
import com.fastpilot.model.Interval
import com.fastpilot.model.Position
import com.fastpilot.model.RaceControlMessage
import com.fastpilot.model.Weather
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * Manages polling flows for real-time data from OpenF1.
 * Each flow emits updated data at the specified interval.
 */
class LivePollingManager(
    private val positionRepo: PositionRepository,
    private val intervalRepo: IntervalRepository,
    private val raceControlRepo: RaceControlRepository,
    private val weatherRepo: WeatherRepository
) {
    // Intervals from the TCC I (§4.5). Live requests share one OpenF1 Sponsor quota (60 per
    // minute) through the proxy, whose CDN cache answers repeated requests; with every live
    // screen open: positions 15 + intervals 15 + race control 6 + pit stops and overtakes 8
    // + radio 4 + weather 1 = 49 requests per minute.
    companion object {
        const val POSITION_POLL_MS = 4_000L
        const val RACE_CONTROL_POLL_MS = 10_000L
        const val RACE_EVENTS_POLL_MS = 15_000L
        const val TEAM_RADIO_POLL_MS = 15_000L
        const val WEATHER_POLL_MS = 60_000L
    }

    fun observePositions(sessionKey: Int): Flow<List<Position>> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val positions = positionRepo.getLatestPositions(sessionKey)
                emit(positions)
            } catch (_: Exception) { /* retry on next poll */ }
            delay(POSITION_POLL_MS)
        }
    }

    fun observeIntervals(sessionKey: Int): Flow<List<Interval>> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val intervals = intervalRepo.getLatestIntervals(sessionKey)
                emit(intervals)
            } catch (_: Exception) { /* retry on next poll */ }
            delay(POSITION_POLL_MS)
        }
    }

    fun observeRaceControl(sessionKey: Int): Flow<List<RaceControlMessage>> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val messages = raceControlRepo.getRaceControl(sessionKey)
                emit(messages)
            } catch (_: Exception) { /* retry on next poll */ }
            delay(RACE_CONTROL_POLL_MS)
        }
    }

    fun observeWeather(sessionKey: Int): Flow<Weather?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val weather = weatherRepo.getLatestWeather(sessionKey)
                emit(weather)
            } catch (_: Exception) { /* retry on next poll */ }
            delay(WEATHER_POLL_MS)
        }
    }
}
