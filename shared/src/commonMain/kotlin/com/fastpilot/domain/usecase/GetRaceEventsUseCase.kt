package com.fastpilot.domain.usecase

import com.fastpilot.data.repository.OvertakeRepository
import com.fastpilot.data.repository.PitStopRepository
import com.fastpilot.model.Overtake
import com.fastpilot.model.PitStop
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** One entry of the race events feed (TCC I §4.6): a pit stop or an overtake. */
data class RaceEvent(
    val date: String,
    val type: Type,
    /** The driver who pitted, or who overtook. */
    val driverNumber: Int,
    /** The overtaken driver. */
    val otherDriverNumber: Int? = null,
    /** Position gained by the overtake. */
    val position: Int? = null,
    val lapNumber: Int? = null,
    /** Stationary time of the pit stop, in seconds (OpenF1 has it from 2024 on). */
    val stopDuration: Double? = null,
    /** Time spent in the pit lane, in seconds. */
    val laneDuration: Double? = null
) {
    enum class Type { PIT_STOP, OVERTAKE }
}

class GetRaceEventsUseCase(
    private val pitStopRepo: PitStopRepository,
    private val overtakeRepo: OvertakeRepository
) {
    companion object {
        const val MAX_EVENTS = 30

        /** Pit stops and overtakes as one feed, newest first, at most [MAX_EVENTS]. */
        fun merge(pitStops: List<PitStop>, overtakes: List<Overtake>): List<RaceEvent> {
            val pits = pitStops.mapNotNull { pit ->
                RaceEvent(
                    date = pit.date ?: return@mapNotNull null,
                    type = RaceEvent.Type.PIT_STOP,
                    driverNumber = pit.driverNumber,
                    lapNumber = pit.lapNumber,
                    stopDuration = pit.stopDuration,
                    laneDuration = pit.laneDuration ?: pit.pitDuration
                )
            }
            val passes = overtakes.mapNotNull { overtake ->
                RaceEvent(
                    date = overtake.date ?: return@mapNotNull null,
                    type = RaceEvent.Type.OVERTAKE,
                    driverNumber = overtake.overtakingDriverNumber,
                    otherDriverNumber = overtake.overtakenDriverNumber,
                    position = overtake.position
                )
            }
            // ISO-8601 dates with the same offset sort as text
            return (pits + passes).sortedByDescending { it.date }.take(MAX_EVENTS)
        }
    }

    /** The session's events, newest first; a source that fails is left out. */
    suspend operator fun invoke(sessionKey: Int): List<RaceEvent> = coroutineScope {
        val pitStops = async { orEmpty { pitStopRepo.getPitStops(sessionKey) } }
        val overtakes = async { orEmpty { overtakeRepo.getOvertakes(sessionKey) } }
        merge(pitStops.await(), overtakes.await())
    }

    /** [invoke] every [pollIntervalMs] while collected (live sessions). */
    fun observe(sessionKey: Int, pollIntervalMs: Long): Flow<List<RaceEvent>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(invoke(sessionKey))
            delay(pollIntervalMs)
        }
    }

    private suspend fun <T> orEmpty(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }
}
