package com.fastpilot.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** The signed-in Firebase user, as shown in the app (the account menu). */
data class AuthUser(val uid: String, val name: String?, val email: String?, val photoUrl: String?)

sealed interface SignInResult {
    data class Success(val user: AuthUser) : SignInResult
    /** The user closed the Google account picker: nothing to show. */
    data object Cancelled : SignInResult
    data class Failed(val detail: String) : SignInResult
}

/**
 * Google sign-in through Firebase Authentication, one implementation per platform:
 * Android (Credential Manager), iOS (GoogleSignIn, in Swift), Web (Firebase JS SDK, in index.html).
 * Desktop uses [NoAuth], so it only offers guest mode.
 */
interface AuthService {
    /** False where sign-in is not set up: the Google button and the account menu are hidden. */
    val isAvailable: Boolean

    /** Firebase keeps the session on the device, so a user who signed in before is returned here. */
    fun currentUser(): AuthUser?

    /** Waits for the saved session to load (asynchronous on Web, where Firebase reads it from IndexedDB). */
    suspend fun restoreSession(): AuthUser? = currentUser()
    suspend fun signInWithGoogle(): SignInResult
    suspend fun signOut()

    /** Deletes the Firebase account (required by the App Store). Returns the error detail, or null. */
    suspend fun deleteAccount(): String?
}

object NoAuth : AuthService {
    override val isAvailable = false
    override fun currentUser(): AuthUser? = null
    override suspend fun signInWithGoogle(): SignInResult = SignInResult.Failed("Sign-in is not available on this platform")
    override suspend fun signOut() {}
    override suspend fun deleteAccount(): String? = null
}

/** The account as the screens see it; [onLeft] takes the app back to the login screen. */
class AccountSession(val auth: AuthService, private val onLeft: () -> Unit) {
    var user by mutableStateOf(auth.currentUser())
        private set

    suspend fun restore() {
        auth.restoreSession()?.let { user = it }
    }

    suspend fun signInWithGoogle(): SignInResult =
        auth.signInWithGoogle().also { if (it is SignInResult.Success) user = it.user }

    suspend fun signOut() {
        auth.signOut()
        user = null
        onLeft()
    }

    suspend fun deleteAccount(): String? =
        auth.deleteAccount().also { error -> if (error == null) { user = null; onLeft() } }

    /** A guest asking to sign in from the account menu. */
    fun goToLogin() = onLeft()
}

val LocalAccount = staticCompositionLocalOf<AccountSession> { error("AccountSession not provided") }
