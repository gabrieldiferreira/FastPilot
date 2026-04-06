package com.fastpilot.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.fastpilot.db.DatabaseDriverFactory
import com.fastpilot.db.FastPilotDB
import com.fastpilot.ui.App
import com.fastpilot.ui.di.appModules
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {
    // Initialize Koin before the Compose application
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(appModules + module {
                single { DatabaseDriverFactory() }
                single { FastPilotDB(get<DatabaseDriverFactory>().create()) }
            })
        }
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "FastPilot — F1 Telemetry",
            state = rememberWindowState(width = 420.dp, height = 900.dp)
        ) {
            App()
        }
    }
}

