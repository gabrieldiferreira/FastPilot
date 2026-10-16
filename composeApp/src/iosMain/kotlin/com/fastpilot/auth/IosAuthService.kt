package com.fastpilot.auth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implemented in Swift (iosApp/FirebaseAuthBridge.swift): the Firebase and GoogleSignIn SDKs
 * come from Swift Package Manager, so Kotlin reaches them through this interface.
 * Every callback is called once, on the main thread.
 */
interface IosAuthBridge {
    fun currentUser(): AuthUser?
    fun signInWithGoogle(onSuccess: (AuthUser) -> Unit, onError: (String) -> Unit, onCancel: () -> Unit)
    fun signOut()
    /** [onDone] receives the error detail, or null when the account was deleted. */
    fun deleteAccount(onDone: (String?) -> Unit)
}

class IosAuthService(private val bridge: IosAuthBridge) : AuthService {
    override val isAvailable = true

    override fun currentUser(): AuthUser? = bridge.currentUser()

    override suspend fun signInWithGoogle(): SignInResult = suspendCancellableCoroutine { continuation ->
        bridge.signInWithGoogle(
            onSuccess = { continuation.resume(SignInResult.Success(it)) },
            onError = { continuation.resume(SignInResult.Failed(it)) },
            onCancel = { continuation.resume(SignInResult.Cancelled) },
        )
    }

    override suspend fun signOut() = bridge.signOut()

    override suspend fun deleteAccount(): String? = suspendCancellableCoroutine { continuation ->
        bridge.deleteAccount { continuation.resume(it) }
    }
}
