package com.naveenapps.expensemanager.core.data.cloudbackup

import java.util.concurrent.atomic.AtomicBoolean

internal object CloudBackupOperationState {

    private val restoreInProgress = AtomicBoolean(false)
    private val restoreInvalidationObserved = AtomicBoolean(false)
    private val suppressNextBackup = AtomicBoolean(false)

    fun isRestoreInProgress(): Boolean = restoreInProgress.get()

    fun beginRestore() {
        restoreInvalidationObserved.set(false)
        restoreInProgress.set(true)
    }

    fun endRestore() {
        restoreInProgress.set(false)
        suppressNextBackup.set(!restoreInvalidationObserved.get())
    }

    fun observeRestoreInvalidation() {
        restoreInvalidationObserved.set(true)
    }

    fun consumeSuppressedBackup(): Boolean = suppressNextBackup.compareAndSet(true, false)
}