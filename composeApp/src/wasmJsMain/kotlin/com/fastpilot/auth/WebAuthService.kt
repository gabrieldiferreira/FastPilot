package com.fastpilot.auth

import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Google sign-in on Web through the Firebase JS SDK, set up in index.html: it loads the SDK
 * and resolves window.fastpilotAuthReady with the helpers below. They never reject (a JS
 * exception reaches Kotlin/Wasm without its message): failures come back as status "error".
 */
private external interface JsAuthUser : JsAny {
    val uid: String
    val name: String?
    val email: String?
    val photoUrl: String?
}

private external interface JsAuthResult : JsAny {
    /** "ok", "cancelled" or "error" */
    val status: String
    val user: JsAuthUser?
    val detail: String?
}

private external interface JsAuth : JsAny {
    fun current(): JsAuthUser?
    fun signIn(): Promise<JsAuthResult>
    fun signOut(): Promise<JsAny?>
    fun deleteAccount(): Promise<JsAuthResult>
}

/** Rejects when the SDK cannot be downloaded (offline): the app then works as guest only. */
@JsFun("() => window.fastpilotAuthReady")
private external fun authReady(): Promise<JsAuth>

object WebAuthService : AuthService {
    private var auth: JsAuth? = null

    override val isAvailable = true

    override fun currentUser(): AuthUser? = auth?.current()?.toAuthUser()

    override suspend fun restoreSession(): AuthUser? = withTimeoutOrNull(5_000) { loaded() }?.current()?.toAuthUser()

    override suspend fun signInWithGoogle(): SignInResult {
        val auth = loaded() ?: return SignInResult.Failed("Firebase did not load (no connection?)")
        val result = auth.signIn().await<JsAuthResult>()
        return when (result.status) {
            "ok" -> result.user?.let { SignInResult.Success(it.toAuthUser()) } ?: SignInResult.Failed("Firebase returned no user")
            "cancelled" -> SignInResult.Cancelled
            else -> SignInResult.Failed(result.detail ?: "unknown error")
        }
    }

    override suspend fun signOut() {
        loaded()?.signOut()?.await<JsAny?>()
    }

    override suspend fun deleteAccount(): String? {
        val auth = loaded() ?: return "Firebase did not load (no connection?)"
        val result = auth.deleteAccount().await<JsAuthResult>()
        return if (result.status == "ok") null else result.detail ?: "unknown error"
    }

    private suspend fun loaded(): JsAuth? = auth ?: runCatching { authReady().await<JsAuth>() }.getOrNull()?.also { auth = it }
}

private fun JsAuthUser.toAuthUser() = AuthUser(uid, name, email, photoUrl)
