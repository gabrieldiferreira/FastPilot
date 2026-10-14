package com.fastpilot.ui.navigation

import com.fastpilot.ui.i18n.AppLanguage
import com.fastpilot.ui.i18n.EnUsStrings
import com.fastpilot.ui.i18n.EsStrings
import com.fastpilot.ui.i18n.PtBrStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlRouterTest {

    private val tabs = listOf(DashboardTab, TelemetryTab, StrategyTab, DuelTab, RadioTab, WeatherTab, StandingsTab)

    @Test
    fun everyTabHasItsOwnPathInEveryLanguage() {
        assertEquals(
            listOf("/painel", "/telemetria", "/estrategia", "/duelo", "/radio", "/clima", "/classificacao"),
            tabs.map { pathForTab(it, PtBrStrings) }
        )
        assertEquals(
            listOf("/dashboard", "/telemetry", "/strategy", "/duel", "/radio", "/weather", "/standings"),
            tabs.map { pathForTab(it, EnUsStrings) }
        )
        assertEquals(
            listOf("/panel", "/telemetria", "/estrategia", "/duelo", "/radio", "/clima", "/clasificacion"),
            tabs.map { pathForTab(it, EsStrings) }
        )
        for (language in AppLanguage.entries) {
            val paths = tabs.map { pathForTab(it, language.strings) } + language.strings.routes.login
            assertEquals(paths.size, paths.toSet().size, "two screens share a path in $language")
            assertTrue(paths.all { it.matches(Regex("/[a-z-]+")) }, "paths must be plain ASCII: $paths")
        }
    }

    @Test
    fun aPathInAnyLanguageOpensTheSameScreen() {
        assertEquals(StandingsTab.key, tabForPath("/classificacao")?.key)
        assertEquals(StandingsTab.key, tabForPath("/clasificacion")?.key)
        assertEquals(StandingsTab.key, tabForPath("/Standings/")?.key)
        assertEquals(Destination.Login, destinationForPath("/entrar"))
        assertEquals(Destination.Login, destinationForPath("/acceder"))
        assertNull(destinationForPath("/"))
        assertNull(destinationForPath("/unknown"))
    }

    private class FakeRouter(override var currentPath: String) : UrlRouter {
        override val initialPath = currentPath
        val history = mutableListOf(currentPath)
        override fun push(path: String, title: String) { history += path; currentPath = path }
        override fun replace(path: String, title: String) { history[history.lastIndex] = path; currentPath = path }
        override fun onPathChange(listener: (String) -> Unit) {}
    }

    @Test
    fun changingLanguageRewritesTheAddressWithoutANewHistoryEntry() {
        val router = FakeRouter("/telemetry")
        router.show(pathForTab(TelemetryTab, PtBrStrings), "")
        router.show(pathForTab(RadioTab, PtBrStrings), "")
        router.show(pathForTab(RadioTab, EsStrings), "")
        assertEquals(listOf("/telemetria", "/radio"), router.history)
        router.show(pathForTab(StandingsTab, EsStrings), "")
        router.show(pathForTab(StandingsTab, EnUsStrings), "")
        assertEquals(listOf("/telemetria", "/radio", "/standings"), router.history)
    }
}
