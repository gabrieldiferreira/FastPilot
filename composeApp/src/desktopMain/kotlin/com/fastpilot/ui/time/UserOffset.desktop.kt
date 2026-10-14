package com.fastpilot.ui.time

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt

internal actual fun userOffsetAt(instant: Instant): UtcOffset = TimeZone.currentSystemDefault().offsetAt(instant)
