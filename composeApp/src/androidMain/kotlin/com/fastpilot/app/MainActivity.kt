package com.fastpilot.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fastpilot.db.DatabaseDriverFactory
import com.fastpilot.db.FastPilotDB
import com.fastpilot.ui.App
import com.fastpilot.ui.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class FastPilotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FastPilotApplication)
            modules(appModules + module {
                single { DatabaseDriverFactory(get()) }
                single { FastPilotDB(get<DatabaseDriverFactory>().create()) }
            })
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
