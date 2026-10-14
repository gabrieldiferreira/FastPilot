package com.fastpilot.data.repository

import com.fastpilot.domain.usecase.LiveSessionDetector
import com.fastpilot.model.Session
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** First season with OpenF1 data. */
const val FIRST_SEASON = 2023

/** Every season with OpenF1 data, newest first: from the current year down to [FIRST_SEASON]. */
fun availableSeasons(now: Instant = Clock.System.now()): List<Int> =
    (now.toLocalDateTime(TimeZone.UTC).year downTo FIRST_SEASON).toList()

/**
 * Every session of [availableSeasons], newest first, including the ones not run yet
 * (OpenF1 lists the whole calendar in advance). A season that fails to load is skipped.
 */
suspend fun SessionRepository.getSeasonSessions(now: Instant = Clock.System.now()): List<Session> =
    availableSeasons(now).flatMap { year ->
        try {
            getSessions(year)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }.sortedByDescending { it.dateStart.orEmpty() }

/** Sessions of [availableSeasons] that have already started (they have data), newest first. */
suspend fun SessionRepository.getRecentSessions(now: Instant = Clock.System.now()): List<Session> =
    getSeasonSessions(now).startedBy(now)

fun List<Session>.startedBy(now: Instant = Clock.System.now()): List<Session> =
    filter { LiveSessionDetector.hasStarted(it, now) }

/** The session a screen opens with: the live one, else the most recent race, else the newest session. */
fun List<Session>.defaultSession(now: Instant = Clock.System.now()): Session? =
    LiveSessionDetector.findLiveSession(this, now)
        ?: firstOrNull { it.sessionType == "Race" }
        ?: firstOrNull()
