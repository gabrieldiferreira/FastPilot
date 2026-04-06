package com.fastpilot.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.ui.theme.DarkGradientColors
import com.fastpilot.ui.theme.F1Red

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(DarkGradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logotipo
            Text(
                text = "FastPilot",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge
            )
            
            Text(
                text = "Telemetria de elite nas suas mãos",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            // Botão do Google
            Button(
                onClick = {
                    // TODO: Aqui é onde chamaremos a função nativa KMP para abrir o Google Sign In Intent.
                    // Quando o Firebase retornar sucesso, chamamos:
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Ícone simulado do Google (G)
                    Text(
                        text = "G",
                        color = Color.Blue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "Continuar com o Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = { 
                    // ByPass option para desenvolvimento rápido sem Firebase no momento.
                    onLoginSuccess() 
                }
            ) {
                Text(
                    text = "Continuar como Convidado",
                    color = Color.Gray
                )
            }
        }
    }
}
