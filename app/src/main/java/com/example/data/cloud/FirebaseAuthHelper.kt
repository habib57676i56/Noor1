package com.example.data.cloud

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data object Cancelled : AuthResult()
    data object NoCredential : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Pure Kotlin login / privileged-action rate limiter (unit-testable).
 * Locks out after 5 consecutive failed attempts within 5 minutes for 2 minutes.
 */
class AdminSecurityRateLimiter(
    private val maxAttempts: Int = 5,
    private val windowMillis: Long = 5 * 60_000L,
    private val lockoutMillis: Long = 2 * 60_000L
) {
    private val failedTimestamps = mutableListOf<Long>()
    private var lockedUntilMillis: Long = 0L

    fun isLocked(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis < lockedUntilMillis

    fun remainingLockSeconds(nowMillis: Long = System.currentTimeMillis()): Long =
        ((lockedUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)

    fun recordFailure(nowMillis: Long = System.currentTimeMillis()): Boolean {
        failedTimestamps.removeAll { nowMillis - it > windowMillis }
        failedTimestamps.add(nowMillis)
        if (failedTimestamps.size >= maxAttempts) {
            lockedUntilMillis = nowMillis + lockoutMillis
            failedTimestamps.clear()
            return true
        }
        return false
    }

    fun recordSuccess() {
        failedTimestamps.clear()
        lockedUntilMillis = 0L
    }

    fun failedAttemptCount(nowMillis: Long = System.currentTimeMillis()): Int {
        failedTimestamps.removeAll { nowMillis - it > windowMillis }
        return failedTimestamps.size
    }
}

/**
 * Google Sign-In Helper via Jetpack Credential Manager + Firebase Auth.
 * Strictly follows single-option-per-request invariant and NonCancellable sign-out.
 */
object FirebaseAuthHelper {
    private const val TAG = "NoorAuthHelper"

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (_: IllegalStateException) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun isBootstrappedOwner(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.email.equals(NoorPlatformConstants.PRIMARY_OWNER_EMAIL, ignoreCase = true) &&
            user.isEmailVerified
    }

    @Suppress("DiscouragedApi")
    private fun resolveDefaultWebClientId(context: Context): String? {
        val packagesToCheck = listOf(context.packageName, "com.example").distinct()
        for (pkg in packagesToCheck) {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", pkg)
            if (resId != 0) {
                val value = runCatching { context.getString(resId) }.getOrNull()
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }

    fun isFirebaseConfigured(context: Context): Boolean =
        resolveDefaultWebClientId(context) != null

    suspend fun attemptSilentAutoSignIn(activityContext: Context): AuthResult {
        val webClientId = resolveDefaultWebClientId(activityContext)
            ?: return AuthResult.NoCredential
        val credentialManager = CredentialManager.create(activityContext)
        return try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            completeFirebaseSignIn(response.credential)
        } catch (_: NoCredentialException) {
            AuthResult.NoCredential
        } catch (_: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: Exception) {
            Log.w(TAG, "Silent sign-in skipped: ${e.message}")
            AuthResult.NoCredential
        }
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): AuthResult {
        val webClientId = resolveDefaultWebClientId(activityContext)
            ?: return AuthResult.Error("Firebase configuration (google-services.json) is missing in this build.")
        val credentialManager = CredentialManager.create(activityContext)
        return try {
            val option = GetSignInWithGoogleOption.Builder(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            completeFirebaseSignIn(response.credential)
        } catch (_: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (_: NoCredentialException) {
            AuthResult.NoCredential
        } catch (e: Exception) {
            Log.e(TAG, "Interactive Google Sign-In failed", e)
            AuthResult.Error(e.message ?: "Google Sign-In failed")
        }
    }

    private suspend fun completeFirebaseSignIn(credential: androidx.credentials.Credential): AuthResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
            val user = authResult.user
            return if (user != null) AuthResult.Success(user) else AuthResult.Error("No authenticated user returned")
        }
        return AuthResult.Error("Unsupported credential type")
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {
            }
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
        }
    }
}
