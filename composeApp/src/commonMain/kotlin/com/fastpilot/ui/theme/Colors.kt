package com.fastpilot.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core F1 Palette ─────────────────────────────────────────────────────
val CarbonBlack = Color(0xFF0D0D0D)
val NightSurface = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF16213E)
val MidSurface = Color(0xFF1E2A47)
val CardSurface = Color(0xFF1C1C2E)
val CardSurfaceHover = Color(0xFF252540)

// ── Primary / Accent ────────────────────────────────────────────────────
val F1Red = Color(0xFFE10600)
val F1RedDark = Color(0xFFB00500)
val DRSGreen = Color(0xFF00D4AA)
val DRSGreenDark = Color(0xFF00A885)
val FlagYellow = Color(0xFFFFD700)
val SafetyCarAmber = Color(0xFFFFA500)
val ChequeredWhite = Color(0xFFF5F5F5)

// ── Tyre Compounds ──────────────────────────────────────────────────────
val TyreSoft = Color(0xFFFF3333)
val TyreMedium = Color(0xFFFFD700)
val TyreHard = Color(0xFFFFFFFF)
val TyreIntermediate = Color(0xFF43B02A)
val TyreWet = Color(0xFF0072CE)

// ── Functional Colors ───────────────────────────────────────────────────
val ThrottleGreen = Color(0xFF00E676)
val BrakeRed = Color(0xFFFF1744)
val SpeedBlue = Color(0xFF448AFF)
val GearPurple = Color(0xFFBB86FC)

// ── Text / UI ───────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFFB0B0B0)
val TextTertiary = Color(0xFF707070)
val DividerColor = Color(0xFF2A2A3A)

// ── Team Colors (2024-2026 Grid) ────────────────────────────────────────
object TeamColors {
    val RedBull = Color(0xFF3671C6)
    val Ferrari = Color(0xFFE8002D)
    val Mercedes = Color(0xFF27F4D2)
    val McLaren = Color(0xFFFF8000)
    val AstonMartin = Color(0xFF229971)
    val Alpine = Color(0xFF0093CC)
    val Williams = Color(0xFF64C4FF)
    val AlphaTauri = Color(0xFF6692FF) // RB / VCARB
    val Haas = Color(0xFFB6BABD)
    val Sauber = Color(0xFF52E252)

    /** Maps Jolpica/Ergast constructor ids (which carry no colour) to team colours. */
    fun fromConstructorId(id: String): Color = when (id) {
        "red_bull" -> RedBull
        "ferrari" -> Ferrari
        "mercedes" -> Mercedes
        "mclaren" -> McLaren
        "aston_martin" -> AstonMartin
        "alpine" -> Alpine
        "williams" -> Williams
        "rb", "alphatauri" -> AlphaTauri
        "haas" -> Haas
        "sauber", "alfa", "audi" -> Sauber
        else -> TextSecondary
    }

    fun fromHex(hex: String): Color {
        return try {
            Color(("FF${hex.removePrefix("#")}").toLong(16))
        } catch (_: Exception) {
            TextSecondary
        }
    }
}

// ── Gradient Helpers ────────────────────────────────────────────────────
val DarkGradientColors = listOf(CarbonBlack, NightSurface)
val CardGradientColors = listOf(CardSurface, DarkSurface)
val RedGlow = listOf(F1Red.copy(alpha = 0.3f), Color.Transparent)
