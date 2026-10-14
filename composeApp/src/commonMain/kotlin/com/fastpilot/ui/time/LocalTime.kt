package com.fastpilot.ui.time

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * The offset of the user's time zone at [instant], daylight saving included. Every time on
 * screen is shown in it: the APIs send UTC, and race control writes the circuit's local time.
 */
internal expect fun userOffsetAt(instant: Instant): UtcOffset

private fun Int.twoDigits() = toString().padStart(2, '0')

private fun LocalTime.format(withSeconds: Boolean) =
    "${hour.twoDigits()}:${minute.twoDigits()}" + if (withSeconds) ":${second.twoDigits()}" else ""

/** "2024-12-08T13:09:54.663+00:00" -> "10:09:54" for a user in UTC-3; null if it does not parse. */
fun localClock(isoDate: String?, offsetAt: (Instant) -> UtcOffset = ::userOffsetAt): String? {
    val instant = isoDate?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return instant.toLocalDateTime(offsetAt(instant).asTimeZone()).time.format(withSeconds = true)
}

/** OpenF1 "gmt_offset" ("08:00:00", "-04:00:00") as an offset; null if absent. */
fun parseGmtOffset(gmtOffset: String?): UtcOffset? {
    val match = gmtOffset?.let { Regex("""(-)?(\d{1,2}):(\d{2})(?::\d{2})?""").matchEntire(it.trim()) } ?: return null
    val sign = if (match.groupValues[1] == "-") -1 else 1
    return UtcOffset(hours = sign * match.groupValues[2].toInt(), minutes = sign * match.groupValues[3].toInt())
}

/** The user's UTC offset now, for the "times in your time zone (UTC-03:00)" note. */
fun userOffsetLabel(now: Instant = Clock.System.now(), offsetAt: (Instant) -> UtcOffset = ::userOffsetAt): String {
    val seconds = offsetAt(now).totalSeconds
    val sign = if (seconds < 0) "−" else "+"
    return "UTC$sign${(abs(seconds) / 3600).twoDigits()}:${(abs(seconds) % 3600 / 60).twoDigits()}"
}

// "21:45:12" or "15:20", not lap times ("1:41.031") nor parts of longer numbers
private val CLOCK = Regex("""(?<![\d:.])([01]?\d|2[0-3]):([0-5]\d)(?::([0-5]\d))?(?![\d:.])""")

/**
 * Race control writes clock times in the circuit's local time ("BLUE FLAG ... TIMED AT 21:45:12",
 * "Q2 WILL RESUME AT 15:20"). Rewrites them in the user's time zone. Only the time of day is
 * written, so each one is taken on the day that puts it closest to when the message was sent.
 */
fun localizeTrackTimes(
    text: String,
    sentAt: Instant,
    trackOffset: UtcOffset,
    offsetAt: (Instant) -> UtcOffset = ::userOffsetAt
): String {
    val trackZone = trackOffset.asTimeZone()
    val trackDate = sentAt.toLocalDateTime(trackZone).date
    return CLOCK.replace(text) { match ->
        val withSeconds = match.groupValues[3].isNotEmpty()
        val time = LocalTime(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toIntOrNull() ?: 0)
        val instant = listOf(-1, 0, 1)
            .map { days -> LocalDateTime(trackDate.plus(days, DateTimeUnit.DAY), time).toInstant(trackZone) }
            .minBy { abs((it - sentAt).inWholeSeconds) }
        instant.toLocalDateTime(offsetAt(instant).asTimeZone()).time.format(withSeconds)
    }
}

/** [message] with its circuit times in the user's time zone, when the message date and the circuit offset are known. */
fun localizeTrackTimes(message: String, sentAt: String?, trackGmtOffset: String?): String {
    val instant = sentAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return message
    val offset = parseGmtOffset(trackGmtOffset) ?: return message
    return localizeTrackTimes(message, instant, offset)
}
