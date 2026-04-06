package com.fastpilot.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.ui.theme.CarbonBlack
import com.fastpilot.ui.theme.DarkGradientColors
import com.fastpilot.ui.theme.F1Red
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val carOffsetX = animateFloatAsState(
        targetValue = if (startAnimation) 0f else -1000f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    val textAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 800)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Wait for animations to finish and show a bit
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(DarkGradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // F1 Car Canvas Animation
            Box(modifier = Modifier.width(200.dp).height(80.dp), contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = carOffsetX.value.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Carbon fiber / tire color
                    val tireColor = Color(0xFF1C1C1C)
                    val chassisColor = F1Red
                    val secondaryColor = Color.White

                    // Tires
                    // Front Left
                    drawRoundRect(
                        color = tireColor,
                        topLeft = Offset(canvasWidth * 0.75f, canvasHeight * 0.15f),
                        size = Size(canvasWidth * 0.15f, canvasHeight * 0.20f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Front Right
                    drawRoundRect(
                        color = tireColor,
                        topLeft = Offset(canvasWidth * 0.75f, canvasHeight * 0.65f),
                        size = Size(canvasWidth * 0.15f, canvasHeight * 0.20f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Rear Left
                    drawRoundRect(
                        color = tireColor,
                        topLeft = Offset(canvasWidth * 0.10f, canvasHeight * 0.10f),
                        size = Size(canvasWidth * 0.20f, canvasHeight * 0.25f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Rear Right
                    drawRoundRect(
                        color = tireColor,
                        topLeft = Offset(canvasWidth * 0.10f, canvasHeight * 0.65f),
                        size = Size(canvasWidth * 0.20f, canvasHeight * 0.25f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Front Wing
                    drawRoundRect(
                        color = chassisColor,
                        topLeft = Offset(canvasWidth * 0.85f, canvasHeight * 0.25f),
                        size = Size(canvasWidth * 0.10f, canvasHeight * 0.50f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Nose
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(canvasWidth * 0.65f, canvasHeight * 0.35f),
                        size = Size(canvasWidth * 0.25f, canvasHeight * 0.30f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )

                    // Main Chassis / Cockpit
                    drawRoundRect(
                        color = chassisColor,
                        topLeft = Offset(canvasWidth * 0.30f, canvasHeight * 0.35f),
                        size = Size(canvasWidth * 0.40f, canvasHeight * 0.30f),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    
                    // Sidepods
                    drawRoundRect(
                        color = chassisColor,
                        topLeft = Offset(canvasWidth * 0.35f, canvasHeight * 0.20f),
                        size = Size(canvasWidth * 0.25f, canvasHeight * 0.60f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )

                    // Cockpit opening (driver)
                    drawRoundRect(
                        color = CarbonBlack,
                        topLeft = Offset(canvasWidth * 0.50f, canvasHeight * 0.42f),
                        size = Size(canvasWidth * 0.10f, canvasHeight * 0.16f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Rear Wing
                    drawRoundRect(
                        color = chassisColor,
                        topLeft = Offset(canvasWidth * 0.05f, canvasHeight * 0.20f),
                        size = Size(canvasWidth * 0.10f, canvasHeight * 0.60f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // FastPilot Text
            Text(
                text = "FastPilot",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
