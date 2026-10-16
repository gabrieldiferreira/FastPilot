package com.fastpilot.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Google account picker (Credential Manager) → Google ID token → Firebase sign-in.
 * The ID token is requested for the project's Web OAuth client, which the google-services
 * plugin turns into the default_web_client_id resource (oauth_client type 3 in google-services.json).
 */
class AndroidAuthService(private val activity: Activity) : AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)

    override val isAvailable = true

    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun signInWithGoogle(): SignInResult = try {
        // Looked up by name: the resource only exists once Google sign-in is set up in Firebase
        val webClientIdRes = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (webClientIdRes == 0) {
            SignInResult.Failed("google-services.json has no Web OAuth client (enable Google in Firebase Authentication)")
        } else {
            val option = GetSignInWithGoogleOption.Builder(activity.getString(webClientIdRes)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentialManager.getCredential(activity, request).credential
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
            user?.let { SignInResult.Success(it.toAuthUser()) } ?: SignInResult.Failed("Firebase returned no user")
        }
    } catch (e: GetCredentialCancellationException) {
        SignInResult.Cancelled
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e("Google sign-in failed", e)
        SignInResult.Failed(e.message ?: e::class.simpleName.orEmpty())
    }

    override suspend fun signOut() {
        auth.signOut()
        // Otherwise the next sign-in picks the same account without asking
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    override suspend fun deleteAccount(): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.delete().await()
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
            null
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            "sign out, sign in again and retry"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: e::class.simpleName.orEmpty()
        }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid, displayName, email, photoUrl?.toString())
