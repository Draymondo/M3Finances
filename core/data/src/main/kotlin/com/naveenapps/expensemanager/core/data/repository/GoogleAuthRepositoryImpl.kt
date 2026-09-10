package com.naveenapps.expensemanager.core.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In via Credential Manager (the modern replacement for the old GoogleSignInClient),
 * paired with Firebase Auth purely as an identity so the cloud backup file can be namespaced per
 * person (see [CloudBackupRepository] and [GoogleAuthRepository]'s doc for why this exists).
 *
 * IMPORTANT — cannot be verified offline: this was written without network access, so the
 * Credential Manager / Google Identity API surface below (option builders, exception types)
 * could not be checked against the live library docs. Double-check against
 * https://developer.android.com/identity/sign-in/credential-manager-siwg once back on a machine
 * with network access, before relying on this.
 */
class GoogleAuthRepositoryImpl(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth,
) : GoogleAuthRepository {

    override fun isSignedIn(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun trySilentSignIn(): Resource<String> {
        return try {
            val webClientId = getWebClientIdOrThrow()
            val credentialManager = CredentialManager.create(context)

            val silentOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()

            val idTokenCredential = requestGoogleIdCredential(credentialManager, silentOption)
            completeFirebaseSignIn(idTokenCredential)
        } catch (exception: Exception) {
            // Expected/normal on a device with no previously-authorized account yet — not
            // logged as a warning to avoid noise on every cold start before the person has ever
            // connected an account.
            Resource.Error(exception)
        }
    }

    override suspend fun signInSilentlyOrPrompt(): Resource<String> {
        val silentResult = trySilentSignIn()
        if (silentResult is Resource.Success) return silentResult

        return try {
            val webClientId = getWebClientIdOrThrow()
            val credentialManager = CredentialManager.create(context)

            val promptOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val idTokenCredential = requestGoogleIdCredential(credentialManager, promptOption)
            completeFirebaseSignIn(idTokenCredential)
        } catch (exception: Exception) {
            Log.w(TAG, "Google sign-in failed", exception)
            Resource.Error(exception)
        }
    }

    private suspend fun completeFirebaseSignIn(
        idTokenCredential: GoogleIdTokenCredential,
    ): Resource<String> {
        val firebaseCredential = GoogleAuthProvider.getCredential(idTokenCredential.idToken, null)
        val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
        val uid = authResult.user?.uid
            ?: return Resource.Error(IllegalStateException("Sign-in succeeded but no user id was returned"))
        return Resource.Success(uid)
    }

    private suspend fun requestGoogleIdCredential(
        credentialManager: CredentialManager,
        option: GetGoogleIdOption,
    ): GoogleIdTokenCredential {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data)
    }

    /**
     * `default_web_client_id` is generated into the *app* module's resources by the
     * google-services Gradle plugin (from google-services.json) — not visible to this module's
     * own R class since core:data is a library the app module depends on, not the other way
     * around. Looking it up by name through the merged runtime resource table sidesteps that
     * module boundary; it works because there is only one merged resource table in the final
     * APK regardless of which module originally declared a resource.
     */
    private fun getWebClientIdOrThrow(): String {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        check(resId != 0) {
            "default_web_client_id not found — is app/google-services.json present and does " +
                "its Firebase project have a Google Sign-In OAuth client configured?"
        }
        return context.getString(resId)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(androidx.credentials.ClearCredentialStateRequest()) }
    }

    companion object {
        private const val TAG = "GoogleAuthRepository"
    }
}
