package com.fastpilot.app

import androidx.compose.ui.window.ComposeUIViewController
import com.fastpilot.db.DatabaseDriverFactory
import com.fastpilot.db.FastPilotDB
import com.fastpilot.ui.App
import com.fastpilot.ui.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

private var koinStarted = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        koinStarted = true
        startKoin {
            modules(
                appModules + module {
                    single { DatabaseDriverFactory() }
                    single { FastPilotDB(get<DatabaseDriverFactory>().create()) }
                }
            )
        }
    }
    return ComposeUIViewController { App() }
}
