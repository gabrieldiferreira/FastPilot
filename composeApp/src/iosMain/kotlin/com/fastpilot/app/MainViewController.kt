package com.fastpilot.app

import androidx.compose.ui.window.ComposeUIViewController
import com.fastpilot.db.DatabaseDriverFactory
import com.fastpilot.db.FastPilotDB
import com.fastpilot.ui.App
import com.fastpilot.ui.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSProcessInfo
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
    // xcrun simctl launch booted com.fastpilot.app -perf "off:5,on:3"
    val arguments = NSProcessInfo.processInfo.arguments.map { it.toString() }
    val benchmark = arguments.indexOf("-perf").takeIf { it >= 0 }
        ?.let { "ios" to arguments.getOrElse(it + 1) { "" } }
    return ComposeUIViewController { App(benchmark) }
}
