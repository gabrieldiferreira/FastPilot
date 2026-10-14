package com.fastpilot.ui.time

import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalTimeTest {

    private val brasilia: (Instant) -> UtcOffset = { UtcOffset(hours = -3) }
    private val madrid: (Instant) -> UtcOffset = { UtcOffset(hours = 2) }

    @Test
    fun apiTimestampsAreShownInTheUserTimeZone() {
        assertEquals("10:45:12", localClock("2026-10-11T13:45:12.094000+00:00", brasilia))
        assertEquals("15:45:12", localClock("2026-10-11T13:45:12+00:00", madrid))
        assertNull(localClock("not a date", brasilia))
    }

    @Test
    fun circuitTimesInsideRaceControlMessagesAreConverted() {
        // Singapore 2026 (UTC+8): sent at 13:45:12 UTC, written as 21:45:12 circuit time
        val sentAt = Instant.parse("2026-10-11T13:45:12+00:00")
        val singapore = UtcOffset(hours = 8)
        assertEquals(
            "BANDEIRA AZUL PARA O CARRO 31 (OCO) ÀS 10:45:12",
            localizeTrackTimes("BANDEIRA AZUL PARA O CARRO 31 (OCO) ÀS 21:45:12", sentAt, singapore, brasilia)
        )
        // Lap times are not clock times; times without seconds stay without seconds
        assertEquals(
            "CARRO 81 (PIA): TEMPO 1:41.031 APAGADO - VOLTA 61 10:43:18",
            localizeTrackTimes("CARRO 81 (PIA): TEMPO 1:41.031 APAGADO - VOLTA 61 21:43:18", sentAt, singapore, brasilia)
        )
        assertEquals("RETOMADA DO Q2 ÀS 16:20", localizeTrackTimes("RETOMADA DO Q2 ÀS 22:20", sentAt, singapore, madrid))
    }

    @Test
    fun aCircuitTimeAcrossMidnightLandsOnTheRightDay() {
        // Las Vegas (UTC-8): sent at 06:05 UTC on Nov 23 = 22:05 on Nov 22 at the circuit
        val sentAt = Instant.parse("2025-11-23T06:05:00+00:00")
        // 23:00 on Nov 22 at UTC-8 = 07:00 UTC on Nov 23 = 04:00 in Brasília
        assertEquals("04:00", localizeTrackTimes("23:00", sentAt, UtcOffset(hours = -8), brasilia))
    }

    @Test
    fun openF1OffsetsAndTheZoneLabel() {
        assertEquals(UtcOffset(hours = 8), parseGmtOffset("08:00:00"))
        assertEquals(UtcOffset(hours = -4), parseGmtOffset("-04:00:00"))
        assertEquals(UtcOffset(hours = 5, minutes = 30), parseGmtOffset("05:30:00"))
        assertNull(parseGmtOffset(null))
        assertEquals("UTC−03:00", userOffsetLabel(Instant.parse("2026-10-14T12:00:00Z"), brasilia))
        // Without the circuit offset the message is left as sent
        assertEquals("ÀS 21:45:12", localizeTrackTimes("ÀS 21:45:12", "2026-10-11T13:45:12+00:00", null))
    }
}
