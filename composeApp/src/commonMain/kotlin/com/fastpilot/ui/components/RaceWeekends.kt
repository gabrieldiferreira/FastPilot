package com.fastpilot.ui.components

import com.fastpilot.model.Session
import com.fastpilot.ui.i18n.Strings

/**
 * One race weekend (OpenF1 "meeting") and its sessions in chronological order.
 * [round] is the championship round; null for pre-season testing.
 */
data class RaceWeekend(
    val key: String,
    val round: Int?,
    val sessions: List<Session>
) {
    private val first get() = sessions.first()
    val isTesting get() = round == null
    fun title(strings: Strings) = if (isTesting) strings.preSeasonTesting else strings.place(first.circuitShortName ?: first.countryName ?: "?")
    fun subtitle(strings: Strings) = listOfNotNull(round?.let { strings.round(it) }, first.countryName?.let { strings.place(it) }).joinToString(" • ")
}

private fun Session.weekendKey() = meetingKey?.toString() ?: "${year}_${circuitShortName ?: countryName}"

private fun Session.isTestingDay() = sessionName?.startsWith("Day") == true

/** Groups the sessions by weekend, most recent weekend first, numbering the rounds of each year. */
fun groupRaceWeekends(sessions: List<Session>): List<RaceWeekend> {
    val chronological = sessions.groupBy { it.weekendKey() }
        .map { (key, list) -> key to list.sortedBy { it.dateStart } }
        .sortedBy { (_, list) -> list.first().dateStart }
    val roundByYear = mutableMapOf<Int?, Int>()
    return chronological.map { (key, list) ->
        val testing = list.all { it.isTestingDay() }
        val round = if (testing) null else {
            val year = list.first().year
            (roundByYear[year] ?: 0).plus(1).also { roundByYear[year] = it }
        }
        RaceWeekend(key, round, list)
    }.reversed()
}

/** Session to open when switching to [weekend]: the same event as [current] if it has one, else the latest. */
fun pickSessionInWeekend(weekend: RaceWeekend, current: Session?): Session =
    weekend.sessions.lastOrNull { it.sessionName == current?.sessionName } ?: weekend.sessions.last()
