package com.fastpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.auth.LocalAccount
import com.fastpilot.ui.i18n.strings
import com.fastpilot.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Account menu: sign out and delete the account (the App Store requires apps with sign-in to
 * offer deletion), or sign in for a guest. Hidden where sign-in is not available (Web, desktop).
 */
@Composable
fun AccountButton() {
    val account = LocalAccount.current
    if (!account.auth.isAvailable) return
    val user = account.user
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Box {
        IconButton(onClick = { menuOpen = true }) {
            val initial = (user?.name ?: user?.email)?.firstOrNull()?.uppercaseChar()
            if (initial != null) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(F1Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = strings.account, tint = TextSecondary, modifier = Modifier.size(32.dp))
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (user == null) {
                DropdownMenuItem(text = { Text(strings.signIn) }, onClick = { menuOpen = false; account.goToLogin() })
            } else {
                (user.name ?: user.email)?.let { title ->
                    DropdownMenuItem(
                        text = {
                            // The item's text slot is a Box: without the Column both lines overlap
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                if (user.name != null && user.email != null) {
                                    Text(user.email, fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        },
                        onClick = {},
                        enabled = false
                    )
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text(strings.signOut) },
                    onClick = { menuOpen = false; scope.launch { account.signOut() } }
                )
                DropdownMenuItem(
                    text = { Text(strings.deleteAccount, color = F1Red) },
                    onClick = { menuOpen = false; deleteError = null; confirmDelete = true }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = false },
            title = { Text(strings.deleteAccountQuestion) },
            text = {
                Text(deleteError?.let(strings::deleteAccountFailed) ?: strings.deleteAccountWarning)
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        scope.launch {
                            deleteError = account.deleteAccount()
                            deleting = false
                            if (deleteError == null) confirmDelete = false
                        }
                    }
                ) { Text(strings.deleteAccount, color = F1Red) }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text(strings.cancel) }
            }
        )
    }
}
