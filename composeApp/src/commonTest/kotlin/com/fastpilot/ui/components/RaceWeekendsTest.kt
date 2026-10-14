package com.fastpilot.ui.components

import com.fastpilot.model.Session
import com.fastpilot.ui.i18n.EnUsStrings
import com.fastpilot.ui.i18n.PtBrStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class RaceWeekendsTest {

    private var nextKey = 1
    private fun session(meeting: Int, name: String, date: String, circuit: String = "Sakhir") = Session(
        sessionKey = nextKey++, sessionName = name, meetingKey = meeting,
        circuitShortName = circuit, countryName = "Bahrain", dateStart = date, year = 2025
    )

    // Newest first, as SessionRepository returns them
    private val sessions = listOf(
        session(3, "Race", "2025-04-13T15:00"),
        session(3, "Qualifying", "2025-04-12T16:00"),
        session(3, "Practice 1", "2025-04-11T11:30"),
        session(2, "Race", "2025-03-16T04:00", "Melbourne"),
        session(2, "Practice 1", "2025-03-14T01:30", "Melbourne"),
        session(1, "Day 1", "2025-02-26T07:00"),
    )

    @Test
    fun groupsByMeetingNewestFirstAndSeparatesTestingAtTheSameCircuit() {
        val weekends = groupRaceWeekends(sessions)
        assertEquals(listOf("3", "2", "1"), weekends.map { it.key })
        assertEquals(listOf(2, 1, null), weekends.map { it.round })
        assertEquals(listOf("Practice 1", "Qualifying", "Race"), weekends[0].sessions.map { it.sessionName })
        assertEquals("Testes de pré-temporada", weekends[2].title(PtBrStrings))
        assertEquals("Etapa 2 • Bahrein", weekends[0].subtitle(PtBrStrings))
        assertEquals("Round 2 • Bahrain", weekends[0].subtitle(EnUsStrings))
    }

    @Test
    fun keepsTheSameEventWhenSwitchingWeekendElseTheLatest() {
        val (bahrain, melbourne, testing) = groupRaceWeekends(sessions)
        val melbourneRace = melbourne.sessions.last()
        assertEquals("Race", pickSessionInWeekend(bahrain, melbourneRace).sessionName)
        assertEquals("Race", pickSessionInWeekend(melbourne, bahrain.sessions[1]).sessionName)
        assertEquals("Day 1", pickSessionInWeekend(testing, melbourneRace).sessionName)
    }
}
