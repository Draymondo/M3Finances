package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.CloudSyncOutcome
import com.naveenapps.expensemanager.core.model.CloudSyncResolution
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Automatic cloud backup, syncing each Room table to its own Firestore collection (one document
 * per row) rather than a single opaque file — chosen over the simpler single-file approach
 * specifically because Firestore documents are capped at 1MB, which a growing SQLite file could
 * eventually exceed.
 *
 * Writes are no longer last-sync-wins. [syncAll] compares this device's last successful sync
 * (DataStore) with Firestore `lastSyncedAt` and whether Room changed since then, then either
 * pushes, restores, no-ops, or blocks with [CloudSyncOutcome.Conflict].
 */
interface CloudBackupRepository {

    fun observeUnresolvedConflict(): Flow<Boolean>

    /**
     * Reconciles local Room with Firestore. Never overwrites a newer cloud copy with a stale
     * phone copy; that case is [CloudSyncOutcome.Conflict] until [resolveSyncConflict].
     * Requires [GoogleAuthRepository.isSignedIn] to be true.
     */
    suspend fun syncAll(): Resource<CloudSyncOutcome>

    /**
     * User chose a side after [CloudSyncOutcome.Conflict]. [CloudSyncResolution.KEEP_LOCAL]
     * pushes the phone (and may delete cloud-only rows). [CloudSyncResolution.USE_CLOUD]
     * replaces the local database with the live cloud copy.
     */
    suspend fun resolveSyncConflict(resolution: CloudSyncResolution): Resource<CloudSyncOutcome>

    /**
     * True if any cloud data exists for the currently signed-in person (used on a fresh install
     * to decide whether there's anything to restore).
     */
    suspend fun hasRemoteBackup(): Resource<Boolean>

    /**
     * Downloads every collection and rebuilds the local database from it (clears local rows
     * first). Used on a fresh install, from Settings, and when resolving a conflict in favour
     * of the cloud.
     */
    suspend fun restoreAllFromCloud(): Resource<Boolean>

    /**
     * Copies the current live cloud state into a dated snapshot (once per calendar day — a
     * no-op if today's snapshot already exists). Snapshots older than 7 days are pruned each
     * time this runs. Called from CloudBackupWorker after a successful local [syncAll] push.
     */
    suspend fun createDailySnapshotIfNeeded(): Resource<Boolean>

    /** Dates (yyyy-MM-dd, newest first) of available snapshots, for the restore-from picker. */
    suspend fun listSnapshotDates(): Resource<List<String>>

    /** Same as [restoreAllFromCloud] but from a specific dated snapshot instead of the live state. */
    suspend fun restoreFromSnapshot(dateKey: String): Resource<Boolean>

    suspend fun getLastBackupTime(): Date?
}
