package com.fastpilot.ui.i18n

import kotlinx.browser.localStorage

// localStorage throws in some private modes; LanguageState falls back to the browser language
internal actual fun loadLanguageTag(): String? = localStorage.getItem("fastpilot.language")

internal actual fun saveLanguageTag(tag: String) {
    localStorage.setItem("fastpilot.language", tag)
}
