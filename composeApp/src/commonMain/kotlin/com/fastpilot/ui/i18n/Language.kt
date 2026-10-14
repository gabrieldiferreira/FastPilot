package com.fastpilot.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

enum class AppLanguage(val tag: String, val nativeName: String, val strings: Strings) {
    PT_BR("pt-BR", "Português", PtBrStrings),
    EN_US("en-US", "English", EnUsStrings),
    ES("es", "Español", EsStrings);

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** The device language when it is one of ours, else Brazilian Portuguese. */
        fun forLocale(language: String): AppLanguage = when (language.lowercase()) {
            "en" -> EN_US
            "es" -> ES
            else -> PT_BR
        }
    }
}

/** The saved language tag, per platform: SharedPreferences, NSUserDefaults, localStorage, Preferences. */
internal expect fun loadLanguageTag(): String?

internal expect fun saveLanguageTag(tag: String)

/** The app language: the saved choice, else the device language. Changing it recomposes every screen. */
object LanguageState {
    var current: AppLanguage by mutableStateOf(
        AppLanguage.fromTag(runCatching { loadLanguageTag() }.getOrNull())
            ?: AppLanguage.forLocale(Locale.current.language)
    )
        private set

    fun select(language: AppLanguage) {
        current = language
        runCatching { saveLanguageTag(language.tag) }
    }
}

val LocalStrings = staticCompositionLocalOf<Strings> { PtBrStrings }

/** The texts of the current language: `strings.loading`. */
val strings: Strings
    @Composable @ReadOnlyComposable get() = LocalStrings.current
