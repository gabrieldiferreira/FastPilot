package com.fastpilot.ui.i18n

import java.util.prefs.Preferences

private val preferences = Preferences.userRoot().node("com/fastpilot")

internal actual fun loadLanguageTag(): String? = preferences.get("language", null)

internal actual fun saveLanguageTag(tag: String) {
    preferences.put("language", tag)
}
