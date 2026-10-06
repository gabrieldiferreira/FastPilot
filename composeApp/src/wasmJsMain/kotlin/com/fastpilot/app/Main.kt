package com.fastpilot.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.fastpilot.ui.App
import com.fastpilot.ui.di.appModules
import kotlinx.browser.document
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // No FastPilotDB binding on Web: SQLDelight has no synchronous browser driver,
    // so repositories run without the local cache and always hit the network.
    startKoin {
        modules(appModules)
    }

    ComposeViewport(document.body!!) {
        App()
    }
}
