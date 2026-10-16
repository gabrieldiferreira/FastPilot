package com.fastpilot.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.fastpilot.auth.LocalAccount
import com.fastpilot.auth.SignInResult
import com.fastpilot.ui.theme.DarkGradientColors
import com.fastpilot.ui.theme.F1Red
import com.fastpilot.ui.i18n.AppLanguage
import com.fastpilot.ui.i18n.Flag
import com.fastpilot.ui.i18n.LanguageState
import com.fastpilot.ui.i18n.strings
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val account = LocalAccount.current
    val scope = rememberCoroutineScope()
    var signingIn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                text = strings.loginTagline,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            // Botão do Google (só onde o login está disponível: Android e iOS)
            if (account.auth.isAvailable) {
                Button(
                    onClick = {
                        error = null
                        signingIn = true
                        scope.launch {
                            when (val result = account.signInWithGoogle()) {
                                is SignInResult.Success -> onLoginSuccess()
                                is SignInResult.Failed -> error = result.detail
                                SignInResult.Cancelled -> {}
                            }
                            signingIn = false
                        }
                    },
                    enabled = !signingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.7f),
                        disabledContentColor = Color.Black
                    )
                ) {
                    if (signingIn) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.Blue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = strings.loginWithGoogle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                error?.let {
                    Text(
                        text = strings.signInFailed(it),
                        color = F1Red,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onLoginSuccess,
                enabled = !signingIn
            ) {
                Text(
                    text = strings.loginAsGuest,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            LanguagePicker()
        }
    }
}

/** One flag per language; the choice is saved and applies to the whole app at once. */
@Composable
private fun LanguagePicker() {
    Text(strings.language, color = Color.Gray, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AppLanguage.entries.forEach { language ->
            val selected = language == LanguageState.current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { LanguageState.select(language) }
                    .semantics { this.selected = selected; role = Role.RadioButton }
                    .padding(8.dp)
            ) {
                Flag(
                    language,
                    Modifier
                        .size(width = 42.dp, height = 28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) F1Red else Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .alpha(if (selected) 1f else 0.6f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    language.nativeName,
                    color = if (selected) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
