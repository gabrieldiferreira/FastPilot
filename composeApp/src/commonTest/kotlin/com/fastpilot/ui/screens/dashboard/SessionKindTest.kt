package com.fastpilot.ui.screens.dashboard

import com.fastpilot.model.Session
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionKindTest {

    @Test
    fun onlyRacesAndSprintsAreRaces() {
        assertTrue(Session(sessionKey = 1, sessionName = "Sprint", sessionType = "Race").isRace())
        assertFalse(Session(sessionKey = 2, sessionName = "Qualifying", sessionType = "Qualifying").isRace())
        assertFalse(Session(sessionKey = 3, sessionName = "Practice 1", sessionType = "Practice").isRace())
    }
}
