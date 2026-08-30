package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Handles the app's single identity concept: "which Google account owns this app's cloud
 * backup". This is deliberately NOT a general-purpose user-account system — the app has no
 * server of its own and no concept of a user profile. The only thing this identity is used for
 * is namespacing the cloud backup file (see [CloudBackupRepository]) so that reinstalling the
 * app on a new device can find the right backup to restore automatically.
 */
interface GoogleAuthRepository {

    /**
     * True once a Google identity has been established for this install (persists across app
     * restarts on the same device; Firebase Auth keeps the session alive locally).
     */
    fun isSignedIn(): Flow<Boolean>

    /**
     * The stable identifier used to namespace this person's cloud backup. Null if not signed in.
     */
    fun getCurrentUserId(): String?

    /**
     * Silent-only: succeeds only when exactly one Google account already authorized for this
     * app is present on the device, with no visible UI at all. Used at app startup (see
     * AppInitializer) where showing an account-picker dialog before the person has even opened
     * the app once would be intrusive. Fails quietly (Resource.Error) otherwise — callers at
     * startup should treat that as "nothing to restore yet", not as an error to surface.
     */
    suspend fun trySilentSignIn(): Resource<String>

    /**
     * Tries the same silent path as [trySilentSignIn] first, then falls back to a one-tap
     * account picker if that doesn't succeed. Intended for an explicit, user-initiated "Connect
     * Google" action (e.g. a Settings button) — not for automatic startup checks.
     */
    suspend fun signInSilentlyOrPrompt(): Resource<String>

    suspend fun signOut()
}
