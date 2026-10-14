package com.fastpilot.ui.i18n

import platform.Foundation.NSUserDefaults

internal actual fun loadLanguageTag(): String? = NSUserDefaults.standardUserDefaults.stringForKey("language")

internal actual fun saveLanguageTag(tag: String) {
    NSUserDefaults.standardUserDefaults.setObject(tag, forKey = "language")
}
