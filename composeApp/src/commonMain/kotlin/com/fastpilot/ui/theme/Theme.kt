package com.fastpilot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FastPilotColorScheme = darkColorScheme(
    primary = F1Red,
    onPrimary = ChequeredWhite,
    primaryContainer = F1RedDark,
    secondary = DRSGreen,
    onSecondary = CarbonBlack,
    secondaryContainer = DRSGreenDark,
    tertiary = FlagYellow,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = BrakeRed
)

@Composable
fun FastPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FastPilotColorScheme,
        content = content
    )
}
