package com.fastpilot.ui.navigation

import cafe.adriel.voyager.navigator.tab.Tab
import com.fastpilot.ui.i18n.AppLanguage
import com.fastpilot.ui.i18n.Routes
import com.fastpilot.ui.i18n.Strings

/**
 * The browser address bar on Web: one path per screen, in the app language ("/telemetria",
 * "/telemetry"), so a screen can be linked, reloaded and reached with the back and forward
 * buttons. Not used on the other platforms.
 */
interface UrlRouter {
    /** The path the app was opened at ("/", "/telemetria"). */
    val initialPath: String

    /** The path in the address bar now. */
    val currentPath: String

    /** Shows [path] in the address bar as a new history entry and sets the page title. */
    fun push(path: String, title: String)

    /** Shows [path] in place of the current entry (same screen, another language) and sets the page title. */
    fun replace(path: String, title: String)

    /** [listener] is called with the path when the user goes back or forward in the history. */
    fun onPathChange(listener: (String) -> Unit)
}

/** A screen reachable by address: the login or one of the tabs. */
sealed interface Destination {
    data object Login : Destination
    data class TabScreen(val tab: Tab) : Destination
}

private fun Routes.paths(): List<Pair<String, Destination>> = listOf(
    login to Destination.Login,
    dashboard to Destination.TabScreen(DashboardTab),
    telemetry to Destination.TabScreen(TelemetryTab),
    strategy to Destination.TabScreen(StrategyTab),
    duel to Destination.TabScreen(DuelTab),
    radio to Destination.TabScreen(RadioTab),
    weather to Destination.TabScreen(WeatherTab),
    standings to Destination.TabScreen(StandingsTab),
)

/** Every path of every language, so a link made in one language opens in another. */
val ALL_PATHS: Set<String> = AppLanguage.entries.flatMap { language -> language.strings.routes.paths().map { it.first } }.toSet()

/** The screen at [path] in any language ("/telemetria", "/telemetry", also "/Telemetry/"), or null. */
fun destinationForPath(path: String): Destination? {
    val normalized = path.trimEnd('/').lowercase()
    return AppLanguage.entries.firstNotNullOfOrNull { language ->
        language.strings.routes.paths().firstOrNull { it.first == normalized }?.second
    }
}

fun tabForPath(path: String): Tab? = (destinationForPath(path) as? Destination.TabScreen)?.tab

/** The path of [tab] in the language of [strings]. */
fun pathForTab(tab: Tab, strings: Strings): String =
    strings.routes.paths().first { (it.second as? Destination.TabScreen)?.tab?.key == tab.key }.first

/**
 * Puts [path] in the address bar: in place of the current entry when it already shows the same
 * screen (in another language or with a trailing slash), as a new history entry otherwise.
 */
fun UrlRouter.show(path: String, title: String) {
    val current = destinationForPath(currentPath)
    when {
        currentPath == path -> replace(path, title)
        current != null && current.sameAs(destinationForPath(path)) -> replace(path, title)
        else -> push(path, title)
    }
}

private fun Destination.sameAs(other: Destination?) = when (this) {
    Destination.Login -> other == Destination.Login
    is Destination.TabScreen -> other is Destination.TabScreen && other.tab.key == tab.key
}
