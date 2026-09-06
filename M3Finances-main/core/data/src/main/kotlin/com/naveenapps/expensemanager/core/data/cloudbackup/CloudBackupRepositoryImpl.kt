package com.naveenapps.expensemanager.core.data.cloudbackup

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.naveenapps.expensemanager.core.database.ExpenseManagerDatabase
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.BudgetDao
import com.naveenapps.expensemanager.core.database.dao.CategoryDao
import com.naveenapps.expensemanager.core.database.dao.DebtDao
import com.naveenapps.expensemanager.core.database.dao.DebtReminderDao
import com.naveenapps.expensemanager.core.database.dao.RecurringTransactionDao
import com.naveenapps.expensemanager.core.database.dao.SavingsGoalDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListItemDao
import com.naveenapps.expensemanager.core.database.dao.TransactionDao
import com.naveenapps.expensemanager.core.datastore.CloudSyncDataStore
import com.naveenapps.expensemanager.core.database.entity.BudgetEntity
import com.naveenapps.expensemanager.core.model.CloudSyncOutcome
import com.naveenapps.expensemanager.core.model.CloudSyncResolution
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * See [CloudBackupRepository]'s doc for why this is structured (one Firestore document per row)
 * rather than a single opaque backup file.
 *
 * IMPORTANT — cannot be verified offline: written without network access, so the Firestore
 * Kotlin API surface below (batch writes, query snapshots) could not be checked against the live
 * SDK docs. Double-check against https://firebase.google.com/docs/firestore/manage-data/add-data
 * once back on a machine with network access.
 *
 * IMPORTANT — Firestore Security Rules (configured in the Firebase console, not in this app's
 * code) must restrict each person's data to themselves, e.g.:
 * ```
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     match /users/{userId}/{document=**} {
 *       allow read, write: if request.auth != null && request.auth.uid == userId;
 *     }
 *   }
 * }
 * ```
 * This project's Firestore is shared with another app (attestation) in the same Firebase
 * project — make sure this rule is scoped under `/users/{userId}/...` specifically and doesn't
 * touch whatever top-level path the other app already uses.
 */
class CloudBackupRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val database: ExpenseManagerDatabase,
    private val googleAuthRepository: GoogleAuthRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val debtReminderDao: DebtReminderDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val cloudBackupScheduler: CloudBackupScheduler,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingListItemDao: ShoppingListItemDao,
    private val cloudSyncDataStore: CloudSyncDataStore,
    private val cloudAppSettingsSync: CloudAppSettingsSync,
) : CloudBackupRepository {

    private val cloudOperationMutex = Mutex()

    private fun requireUserRoot() =
        googleAuthRepository.getCurrentUserId()?.let { uid ->
            firestore.collection("users").document(uid)
        } ?: throw IllegalStateException("Cannot access cloud backup: not signed in")

    override fun observeUnresolvedConflict(): Flow<Boolean> =
        cloudSyncDataStore.observeUnresolvedConflict()

    override suspend fun syncAll(): Resource<CloudSyncOutcome> {
        return try {
            cloudOperationMutex.withLock { reconcileLocked(forcePush = false) }
        } catch (exception: Exception) {
            Log.w(TAG, "Cloud sync failed", exception)
            Resource.Error(exception)
        }
    }

    override suspend fun resolveSyncConflict(
        resolution: CloudSyncResolution,
    ): Resource<CloudSyncOutcome> {
        return try {
            cloudOperationMutex.withLock {
                when (resolution) {
                    CloudSyncResolution.KEEP_LOCAL -> reconcileLocked(forcePush = true)
                    CloudSyncResolution.USE_CLOUD -> restoreLocked()
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Cloud sync conflict resolution failed", exception)
            Resource.Error(exception)
        }
    }

    /**
     * Must be called while [cloudOperationMutex] is held.
     *
     * Cloud newer + local changes (or unknown baseline after an upgrade) → block.
     * Cloud newer + no local changes → restore.
     * Otherwise push if Room changed (or forcePush), else no-op.
     */
    private suspend fun reconcileLocked(forcePush: Boolean): Resource<CloudSyncOutcome> {
        if (forcePush) {
            return pushLocked()
        }
        if (cloudSyncDataStore.hasUnresolvedConflict()) {
            return Resource.Success(CloudSyncOutcome.Conflict)
        }

        val localTs = cloudSyncDataStore.getLastLocalSyncedAt()
        val cloudTs = cloudLastSyncedAtMillis()
        val localModified = hasLocalModifications()

        if (cloudTs > localTs) {
            if (localModified) {
                cloudSyncDataStore.setUnresolvedConflict(true)
                Log.w(TAG, "Blocking syncAll: cloud is newer and this device has local changes")
                return Resource.Success(CloudSyncOutcome.Conflict)
            }
            return restoreLocked()
        }

        if (!localModified) {
            return Resource.Success(CloudSyncOutcome.NoOp)
        }
        return pushLocked()
    }

    private suspend fun hasLocalModifications(): Boolean {
        if (cloudSyncDataStore.hasLocalChangesSinceSync()) return true
        // Upgrade / first run on a phone that already has Room data: there is no DataStore
        // baseline, so treat existing rows as potential local edits rather than restoring
        // or pushing silently.
        if (cloudSyncDataStore.getLastLocalSyncedAt() != 0L) return false
        return accountDao.getAllAccountEntities().isNotEmpty() ||
            transactionDao.getAllTransactionEntities().isNotEmpty() ||
            shoppingListDao.getAllEntities().isNotEmpty()
    }

    private suspend fun cloudLastSyncedAtMillis(): Long {
        val metaDoc = requireUserRoot().collection(META).document("info").get().await()
        return (metaDoc.get("lastSyncedAt") as? Number)?.toLong() ?: 0L
    }

    private suspend fun pushLocked(): Resource<CloudSyncOutcome> {
        val syncedAt = Date().time
        syncAllInternal(syncedAt)
        cloudSyncDataStore.setLastLocalSyncedAt(syncedAt)
        return Resource.Success(CloudSyncOutcome.Pushed)
    }

    private suspend fun restoreLocked(): Resource<CloudSyncOutcome> {
        CloudBackupOperationState.beginRestore()
        cloudBackupScheduler.cancelPendingBackup()
        try {
            restoreFrom(requireUserRoot())
            val ts = cloudLastSyncedAtMillis().takeIf { it > 0L } ?: Date().time
            cloudSyncDataStore.setLastLocalSyncedAt(ts)
            return Resource.Success(CloudSyncOutcome.Restored)
        } finally {
            CloudBackupOperationState.endRestore()
        }
    }

    private suspend fun syncAllInternal(syncedAt: Long) {
            val userRoot = requireUserRoot()

            syncCollection(
                collectionRef = userRoot.collection(ACCOUNTS),
                localRows = accountDao.getAllAccountEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(CATEGORIES),
                localRows = categoryDao.getAllValues().orEmpty(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(TRANSACTIONS),
                localRows = transactionDao.getAllTransactionEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(TRANSACTION_SPLIT_ITEMS),
                localRows = transactionDao.getAllSplitItems(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )

            val budgets = budgetDao.getAllBudgetEntities()
            syncCollection(
                collectionRef = userRoot.collection(BUDGETS),
                localRows = budgets,
                idOf = { it.id },
                toMap = { budget ->
                    val categoryIds = budgetDao.getBudgetCategories(budget.id)
                        .orEmpty().map { it.categoryId }
                    val accountIds = budgetDao.getBudgetAccounts(budget.id)
                        .orEmpty().map { it.accountId }
                    budget.toFirestoreMap(categoryIds, accountIds)
                },
            )

            syncCollection(
                collectionRef = userRoot.collection(DEBTS),
                localRows = debtDao.getAllDebtEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(DEBT_REMINDERS),
                localRows = debtReminderDao.getAllDebtReminderEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(SAVINGS_GOALS),
                localRows = savingsGoalDao.getAllSavingsGoalEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(RECURRING_TRANSACTIONS),
                localRows = recurringTransactionDao.getAllRecurringTransactionEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(SHOPPING_LISTS),
                localRows = shoppingListDao.getAllEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(SHOPPING_LIST_ITEMS),
                localRows = shoppingListItemDao.getAllEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )

            userRoot.collection(SETTINGS).document(APP_SETTINGS)
                .set(cloudAppSettingsSync.read())

            userRoot.collection(META).document("info")
                .set(mapOf("lastSyncedAt" to syncedAt))
                .await()
    }

    /**
     * Upserts every local row and deletes any remote document that no longer has a matching
     * local row (i.e. was deleted locally since the last sync). Firestore batches are capped at
     * 500 operations, so writes are chunked well under that to leave room for the accompanying
     * deletes in the same batch.
     */
    private suspend fun <T> syncCollection(
        collectionRef: com.google.firebase.firestore.CollectionReference,
        localRows: List<T>,
        idOf: (T) -> String,
        toMap: suspend (T) -> Map<String, Any?>,
    ) {
        val remoteIds = collectionRef.get().await().documents.map { it.id }.toSet()
        val localIds = localRows.map(idOf).toSet()
        val staleRemoteIds = remoteIds - localIds

        val chunkSize = 400
        localRows.chunked(chunkSize).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { row -> batch.set(collectionRef.document(idOf(row)), toMap(row)) }
            batch.commit().await()
        }
        staleRemoteIds.chunked(chunkSize).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { id -> batch.delete(collectionRef.document(id)) }
            batch.commit().await()
        }
    }

    override suspend fun hasRemoteBackup(): Resource<Boolean> {
        return try {
            val metaDoc = requireUserRoot().collection(META).document("info").get().await()
            Resource.Success(metaDoc.exists())
        } catch (exception: Exception) {
            Log.w(TAG, "Checking for cloud backup failed", exception)
            Resource.Error(exception)
        }
    }

    override suspend fun restoreAllFromCloud(): Resource<Boolean> {
        return try {
            cloudOperationMutex.withLock { restoreLocked() }
            Resource.Success(true)
        } catch (exception: Exception) {
            Log.w(TAG, "Cloud restore failed", exception)
            Resource.Error(exception)
        }
    }

    override suspend fun restoreFromSnapshot(dateKey: String): Resource<Boolean> {
        CloudBackupOperationState.beginRestore()
        cloudBackupScheduler.cancelPendingBackup()
        return try {
            cloudOperationMutex.withLock {
                val snapshotRoot = requireUserRoot().collection(SNAPSHOTS).document(dateKey)
                check(isSnapshotComplete(snapshotRoot)) { "Snapshot $dateKey is incomplete" }
                restoreFrom(snapshotRoot)
                val ts = Date().time
                cloudSyncDataStore.setLastLocalSyncedAt(ts)
            }
            Resource.Success(true)
        } catch (exception: Exception) {
            Log.w(TAG, "Restore from snapshot $dateKey failed", exception)
            Resource.Error(exception)
        } finally {
            CloudBackupOperationState.endRestore()
        }
    }

    /**
     * Shared by [restoreAllFromCloud] (reads from the live per-user root) and
     * [restoreFromSnapshot] (reads from a dated snapshot under that same root) — both have
     * identical collection layouts, just rooted at a different document.
     */
    private suspend fun restoreFrom(root: com.google.firebase.firestore.DocumentReference) {
        val accounts = root.collection(ACCOUNTS).get().await().documents.map { doc ->
            accountEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val categories = root.collection(CATEGORIES).get().await().documents.map { doc ->
            categoryEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val transactions = root.collection(TRANSACTIONS).get().await().documents.map { doc ->
            transactionEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val splitItems = root.collection(TRANSACTION_SPLIT_ITEMS).get().await().documents.map { doc ->
            transactionSplitItemEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val budgets = root.collection(BUDGETS).get().await().documents.map { doc ->
            val map = doc.data.orEmpty()
            Triple(
                budgetEntityFromFirestoreMap(doc.id, map),
                budgetCategoryIdsFromFirestoreMap(map),
                budgetAccountIdsFromFirestoreMap(map),
            )
        }
        val debts = root.collection(DEBTS).get().await().documents.map { doc ->
            debtEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val debtReminders = root.collection(DEBT_REMINDERS).get().await().documents.map { doc ->
            debtReminderEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val savingsGoals = root.collection(SAVINGS_GOALS).get().await().documents.map { doc ->
            savingsGoalEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val recurringTransactions = root.collection(RECURRING_TRANSACTIONS).get().await().documents.map { doc ->
            recurringTransactionEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val shoppingLists = root.collection(SHOPPING_LISTS).get().await().documents.map { doc ->
            shoppingListEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val shoppingListItems = root.collection(SHOPPING_LIST_ITEMS).get().await().documents.map { doc ->
            shoppingListItemEntityFromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        val appSettings = root.collection(SETTINGS).document(APP_SETTINGS).get().await()

        database.withTransaction {
            transactionDao.deleteAllSplitItems()
            budgetDao.deleteAllBudgetCategories()
            budgetDao.deleteAllBudgetAccounts()
            debtReminderDao.deleteAll()
            transactionDao.deleteAllTransactions()
            budgetDao.deleteAllBudgets()
            debtDao.deleteAll()
            savingsGoalDao.deleteAll()
            recurringTransactionDao.deleteAll()
            shoppingListItemDao.deleteAll()
            shoppingListDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()

            accounts.forEach { accountDao.insert(it) }
            categories.forEach { categoryDao.insert(it) }
            transactions.forEach { transactionDao.insert(it) }
            transactionDao.insertSplitItems(splitItems)
            budgets.forEach { (budget, categoryIds, accountIds) ->
                budgetDao.insertBudget(budget, categoryIds, accountIds)
            }
            debts.forEach { debtDao.insert(it) }
            debtReminders.forEach { debtReminderDao.insert(it) }
            savingsGoals.forEach { savingsGoalDao.insert(it) }
            recurringTransactions.forEach { recurringTransactionDao.insert(it) }
            shoppingLists.forEach { shoppingListDao.insert(it) }
            shoppingListItems.forEach { shoppingListItemDao.insert(it) }
        }

        if (appSettings.exists()) {
            cloudAppSettingsSync.restore(appSettings.data.orEmpty())
        }
    }

    override suspend fun createDailySnapshotIfNeeded(): Resource<Boolean> {
        return cloudOperationMutex.withLock {
            try {
            val userRoot = requireUserRoot()
            val dateKey = todayDateKey()
            val snapshotRoot = userRoot.collection(SNAPSHOTS).document(dateKey)

            deleteSnapshot(snapshotRoot)
            snapshotRoot.collection(META).document("info")
                .set(mapOf("status" to SNAPSHOT_CREATING, "createdAt" to Date().time))
                .await()
            copyCollection(userRoot.collection(ACCOUNTS), snapshotRoot.collection(ACCOUNTS))
            copyCollection(userRoot.collection(CATEGORIES), snapshotRoot.collection(CATEGORIES))
            copyCollection(userRoot.collection(TRANSACTIONS), snapshotRoot.collection(TRANSACTIONS))
            copyCollection(
                userRoot.collection(TRANSACTION_SPLIT_ITEMS),
                snapshotRoot.collection(TRANSACTION_SPLIT_ITEMS),
            )
            copyCollection(userRoot.collection(BUDGETS), snapshotRoot.collection(BUDGETS))
            copyCollection(userRoot.collection(DEBTS), snapshotRoot.collection(DEBTS))
            copyCollection(userRoot.collection(DEBT_REMINDERS), snapshotRoot.collection(DEBT_REMINDERS))
            copyCollection(userRoot.collection(SAVINGS_GOALS), snapshotRoot.collection(SAVINGS_GOALS))
            copyCollection(
                userRoot.collection(RECURRING_TRANSACTIONS),
                snapshotRoot.collection(RECURRING_TRANSACTIONS),
            )
            copyCollection(userRoot.collection(SHOPPING_LISTS), snapshotRoot.collection(SHOPPING_LISTS))
            copyCollection(
                userRoot.collection(SHOPPING_LIST_ITEMS),
                snapshotRoot.collection(SHOPPING_LIST_ITEMS),
            )
            copyCollection(
                userRoot.collection(SETTINGS),
                snapshotRoot.collection(SETTINGS),
            )
            snapshotRoot.collection(META).document("info")
                .set(mapOf("status" to SNAPSHOT_COMPLETE, "createdAt" to Date().time))
                .await()

            pruneOldSnapshots(userRoot)
            Resource.Success(true)
            } catch (exception: Exception) {
            runCatching {
                deleteSnapshot(
                    requireUserRoot().collection(SNAPSHOTS).document(todayDateKey()),
                )
            }
            Log.w(TAG, "Daily snapshot failed", exception)
            Resource.Error(exception)
            }
        }
    }

    override suspend fun listSnapshotDates(): Resource<List<String>> {
        return try {
            val dates = requireUserRoot().collection(SNAPSHOTS).get().await()
                .documents.mapNotNull { snapshot ->
                    val isComplete = isSnapshotComplete(snapshot.reference)
                    snapshot.id.takeIf { isComplete }
                }.sortedDescending()
            Resource.Success(dates)
        } catch (exception: Exception) {
            Resource.Error(exception)
        }
    }

    private suspend fun isSnapshotComplete(
        snapshotRoot: com.google.firebase.firestore.DocumentReference,
    ): Boolean {
        val info = snapshotRoot.collection(META).document("info").get().await()
        return info.getString("status") == SNAPSHOT_COMPLETE
    }

    private suspend fun copyCollection(
        source: com.google.firebase.firestore.CollectionReference,
        destination: com.google.firebase.firestore.CollectionReference,
    ) {
        val docs = source.get().await().documents
        docs.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.set(destination.document(doc.id), doc.data.orEmpty()) }
            batch.commit().await()
        }
    }

    private suspend fun pruneOldSnapshots(userRoot: com.google.firebase.firestore.DocumentReference) {
        val cutoff = Date().time - SNAPSHOT_RETENTION_DAYS * 24L * 60 * 60 * 1000
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        userRoot.collection(SNAPSHOTS).get().await().documents.forEach { snapshotDoc ->
            val snapshotTime = runCatching { dateFormat.parse(snapshotDoc.id)?.time }.getOrNull()
            if (snapshotTime != null && snapshotTime < cutoff) {
                deleteSnapshot(snapshotDoc.reference)
            }
        }
    }

    private suspend fun deleteSnapshot(snapshotRef: com.google.firebase.firestore.DocumentReference) {
        val subcollections = listOf(
            ACCOUNTS, CATEGORIES, TRANSACTIONS, TRANSACTION_SPLIT_ITEMS, BUDGETS,
            DEBTS, DEBT_REMINDERS, SAVINGS_GOALS, RECURRING_TRANSACTIONS,
            SHOPPING_LISTS, SHOPPING_LIST_ITEMS, SETTINGS, META,
        )
        subcollections.forEach { name ->
            snapshotRef.collection(name).get().await().documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        snapshotRef.delete().await()
    }

    private fun todayDateKey(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return dateFormat.format(Date())
    }

    override suspend fun getLastBackupTime(): Date? {
        return runCatching {
            val metaDoc = requireUserRoot().collection(META).document("info").get().await()
            (metaDoc.get("lastSyncedAt") as? Number)?.toLong()?.let { Date(it) }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "CloudBackupRepository"
        private const val ACCOUNTS = "accounts"
        private const val CATEGORIES = "categories"
        private const val BUDGETS = "budgets"
        private const val TRANSACTIONS = "transactions"
        private const val TRANSACTION_SPLIT_ITEMS = "transaction_split_items"
        private const val DEBTS = "debts"
        private const val DEBT_REMINDERS = "debt_reminders"
        private const val SAVINGS_GOALS = "savings_goals"
        private const val RECURRING_TRANSACTIONS = "recurring_transactions"
        private const val SHOPPING_LISTS = "shopping_lists"
        private const val SHOPPING_LIST_ITEMS = "shopping_list_items"
        private const val SETTINGS = "settings"
        private const val APP_SETTINGS = "app"
        private const val META = "meta"
        private const val SNAPSHOTS = "snapshots"
        private const val SNAPSHOT_RETENTION_DAYS = 7
        private const val SNAPSHOT_CREATING = "creating"
        private const val SNAPSHOT_COMPLETE = "complete"
    }
}
