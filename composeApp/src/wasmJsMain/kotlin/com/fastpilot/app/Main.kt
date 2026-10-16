package com.fastpilot.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.fastpilot.auth.WebAuthService
import com.fastpilot.ui.App
import com.fastpilot.ui.navigation.UrlRouter
import com.fastpilot.ui.di.appModules
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // No FastPilotDB binding on Web: SQLDelight has no synchronous browser driver,
    // so repositories run without the local cache and always hit the network.
    startKoin {
        modules(appModules)
    }

    // index.html?perf=off:5,on:3
    val benchmark = window.location.search.removePrefix("?").split('&')
        .firstOrNull { it == "perf" || it.startsWith("perf=") }
        ?.let { "web" to it.substringAfter('=', "").replace("%3A", ":").replace("%2C", ",") }
    ComposeViewport(document.body!!) {
        App(benchmark, BrowserRouter, WebAuthService)
    }
}

/** The address bar: one path per screen, with the browser history (History API). */
private object BrowserRouter : UrlRouter {
    override val initialPath: String = window.location.pathname

    override val currentPath: String get() = window.location.pathname

    override fun push(path: String, title: String) {
        document.title = title
        window.history.pushState(null, title, path)
    }

    override fun replace(path: String, title: String) {
        document.title = title
        // Keeps "?perf=..." and the like
        window.history.replaceState(null, title, path + window.location.search)
    }

    override fun onPathChange(listener: (String) -> Unit) {
        window.addEventListener("popstate") { listener(window.location.pathname) }
    }
}
