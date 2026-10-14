package com.fastpilot.ui.i18n

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The flag of [language], drawn: emoji flags need a color emoji font, which the Web build
 * does not have. Simplified designs (no stars, text or coat of arms) that read at icon size.
 */
@Composable
fun Flag(language: AppLanguage, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (language) {
            AppLanguage.PT_BR -> drawBrazil()
            AppLanguage.EN_US -> drawUsa()
            AppLanguage.ES -> drawSpain()
        }
    }
}

private fun DrawScope.drawBrazil() {
    val (w, h) = size
    drawRect(Color(0xFF009C3B))
    val margin = w * 0.085f
    val rhombus = Path().apply {
        moveTo(w / 2, h * 0.11f)
        lineTo(w - margin, h / 2)
        lineTo(w / 2, h * 0.89f)
        lineTo(margin, h / 2)
        close()
    }
    drawPath(rhombus, Color(0xFFFFDF00))
    drawCircle(Color(0xFF002776), radius = h * 0.25f, center = Offset(w / 2, h / 2))
}

private fun DrawScope.drawUsa() {
    val (w, h) = size
    val stripe = h / 13
    drawRect(Color.White)
    for (i in 0 until 13 step 2) {
        drawRect(Color(0xFFB22234), topLeft = Offset(0f, i * stripe), size = Size(w, stripe))
    }
    // Canton over the first 7 stripes, with a few dots for the stars
    val cantonW = w * 0.4f
    val cantonH = stripe * 7
    drawRect(Color(0xFF3C3B6E), size = Size(cantonW, cantonH))
    val rows = 4
    val cols = 5
    for (r in 0 until rows) for (c in 0 until cols) {
        drawCircle(
            Color.White,
            radius = cantonH / 22,
            center = Offset(cantonW * (c + 0.5f) / cols, cantonH * (r + 0.5f) / rows)
        )
    }
}

private fun DrawScope.drawSpain() {
    val (w, h) = size
    drawRect(Color(0xFFAA151B))
    drawRect(Color(0xFFF1BF00), topLeft = Offset(0f, h / 4), size = Size(w, h / 2))
}
