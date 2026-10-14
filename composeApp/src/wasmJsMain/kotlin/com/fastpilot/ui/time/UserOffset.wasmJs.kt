package com.fastpilot.ui.time

import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset

// kotlinx-datetime on Wasm only knows UTC and fixed offsets without the time zone database;
// the browser knows the user's zone, and its offset on any date (daylight saving included)
private fun browserOffsetMinutes(epochMillis: Double): Int = js("-new Date(epochMillis).getTimezoneOffset()")

internal actual fun userOffsetAt(instant: Instant): UtcOffset =
    UtcOffset(minutes = browserOffsetMinutes(instant.toEpochMilliseconds().toDouble()))
