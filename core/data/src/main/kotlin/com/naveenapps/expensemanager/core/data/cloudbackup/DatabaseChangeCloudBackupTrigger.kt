package com.naveenapps.expensemanager.core.data.cloudbackup

import androidx.room.InvalidationTracker
import com.naveenapps.expensemanager.core.database.ExpenseManagerDatabase
import com.naveenapps.expensemanager.core.datastore.CloudSyncDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires a debounced cloud backup whenever *any* table changes, by piggybacking on Room's own
 * change-tracking mechanism (used internally for Flow query invalidation) rather than adding a
 * manual "please back up now" call to every single repository write method. This is what makes
 * "back up automatically on every modification" require touching this one file instead of every
 * feature that writes to the database.
 */
class DatabaseChangeCloudBackupTrigger(
    private val database: ExpenseManagerDatabase,
    private val cloudBackupScheduler: CloudBackupScheduler,
    private val cloudSyncDataStore: CloudSyncDataStore,
    private val cloudAppSettingsSync: CloudAppSettingsSync,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allTableNames = arrayOf(
        "account",
        "category",
        "budget",
        "budget_account_relation",
        "budget_category_relation",
        "transaction",
        "transaction_split_item",
        "debt",
        "debt_reminder",
        "savings_goal",
        "recurring_transaction",
        "shopping_list",
        "shopping_list_item",
        "pending_transaction",
    )

    private val observer = object : InvalidationTracker.Observer(allTableNames) {
        override fun onInvalidated(tables: Set<String>) {
            if (CloudBackupOperationState.isRestoreInProgress()) {
                CloudBackupOperationState.observeRestoreInvalidation()
                return
            }
            if (CloudBackupOperationState.consumeSuppressedBackup()) return
            scope.launch { cloudSyncDataStore.markLocalChanged() }
            cloudBackupScheduler.scheduleDebouncedBackup()
        }
    }

    /** Call once, at app startup — see AppInitializer. */
    fun start() {
        database.invalidationTracker.addObserver(observer)
        scope.launch {
            cloudAppSettingsSync.observeChanges().collect {
                if (CloudBackupOperationState.isRestoreInProgress()) return@collect
                cloudSyncDataStore.markLocalChanged()
                cloudBackupScheduler.scheduleDebouncedBackup()
            }
        }
    }
}
