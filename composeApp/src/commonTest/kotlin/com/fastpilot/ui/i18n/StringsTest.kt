package com.fastpilot.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StringsTest {

    @Test
    fun theDeviceLanguagePicksTheAppLanguageWithPortugueseAsFallback() {
        assertEquals(AppLanguage.EN_US, AppLanguage.forLocale("en"))
        assertEquals(AppLanguage.ES, AppLanguage.forLocale("ES"))
        assertEquals(AppLanguage.PT_BR, AppLanguage.forLocale("pt"))
        assertEquals(AppLanguage.PT_BR, AppLanguage.forLocale("fr"))
        assertEquals(AppLanguage.ES, AppLanguage.fromTag("es"))
    }

    @Test
    fun openF1SessionNamesAreTranslated() {
        val names = listOf("Practice 1", "Sprint Qualifying", "Sprint Shootout", "Sprint", "Qualifying", "Race", "Day 2")
        assertEquals(
            listOf("TL1", "Classif. Sprint", "Classif. Sprint", "Sprint", "Classificação", "Corrida", "Dia 2"),
            names.map { PtBrStrings.sessionShort(it) }
        )
        assertEquals(
            listOf("FP1", "Sprint Quali", "Sprint Quali", "Sprint", "Qualifying", "Race", "Day 2"),
            names.map { EnUsStrings.sessionShort(it) }
        )
        assertEquals(
            listOf("Libres 1", "Clasif. Sprint", "Clasif. Sprint", "Sprint", "Clasificación", "Carrera", "Día 2"),
            names.map { EsStrings.sessionShort(it) }
        )
        // An unknown name is shown as OpenF1 sends it
        assertEquals("Shakedown", EsStrings.sessionLong("Shakedown"))
    }

    @Test
    fun errorsKeepTheTechnicalDetail() {
        val error = UiError(ErrorKind.STANDINGS, "timeout")
        assertEquals("Erro ao carregar classificação: timeout", PtBrStrings.error(error))
        assertEquals("Could not load the standings: timeout", EnUsStrings.error(error))
        assertEquals("Error al cargar la clasificación: timeout", EsStrings.error(error))
        assertEquals("No sessions found. Check your connection.", EnUsStrings.error(UiError(ErrorKind.NO_SESSIONS, "x")))
    }

    @Test
    fun everyLanguageHasItsOwnTexts() {
        val samples: (Strings) -> List<String> = {
            listOf(it.loginAsGuest, it.tabStandings, it.loadingData, it.strategyTitle, it.radioTitle,
                it.weatherTitle, it.cleanPaceHint, it.lapVsLap(3, "VER", 4, "NOR"), it.radioCount(2))
        }
        val (pt, en, es) = AppLanguage.entries.map { samples(it.strings) }
        pt.indices.forEach { i ->
            assertNotEquals(pt[i], en[i])
            assertNotEquals(en[i], es[i])
        }
    }

    @Test
    fun lappedCarGapsAreTranslated() {
        assertEquals("+1 VOLTA", PtBrStrings.gap("+1 LAP"))
        assertEquals("+2 VUELTAS", EsStrings.gap("+2 LAPS"))
        assertEquals("+2 LAPS", EnUsStrings.gap("+2 LAPS"))
        assertEquals("24.573", PtBrStrings.gap("24.573"))
    }

    @Test
    fun decimalsFollowTheLanguage() {
        assertEquals("2,4", oneDecimal(2.43, PtBrStrings.decimalSeparator))
        assertEquals("24.0", oneDecimal(23.96, EnUsStrings.decimalSeparator))
    }
}
