package com.fastpilot.ui.i18n

import android.content.Context
import org.koin.core.context.GlobalContext

// The application context Koin was started with (FastPilotApplication)
private val preferences
    get() = GlobalContext.get().get<Context>().getSharedPreferences("fastpilot", Context.MODE_PRIVATE)

internal actual fun loadLanguageTag(): String? = preferences.getString("language", null)

internal actual fun saveLanguageTag(tag: String) {
    preferences.edit().putString("language", tag).apply()
}
