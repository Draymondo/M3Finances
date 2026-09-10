package com.naveenapps.expensemanager.core.data.cloudbackup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Debounces automatic cloud backups: every call replaces any still-pending delayed backup with
 * a new one, so a burst of edits (e.g. entering several transactions in a row) results in one
 * upload a few seconds after the *last* edit, not one per edit. Mirrors the existing
 * NotificationScheduler's use of `enqueueUniqueWork` + `ExistingWorkPolicy.REPLACE` for the
 * same reason.
 *
 * Exact debounce delay and background-vs-foreground triggering are intentionally left open —
 * see the project notes; [DEBOUNCE_DELAY_SECONDS] is a reasonable starting point to tune once
 * this is actually running on a device.
 */
class CloudBackupScheduler(
    private val context: Context,
) {

    fun scheduleDebouncedBackup() {
        val request = OneTimeWorkRequestBuilder<CloudBackupWorker>()
            .setInitialDelay(DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(CLOUD_BACKUP_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelPendingBackup() {
        WorkManager.getInstance(context).cancelUniqueWork(CLOUD_BACKUP_WORK_NAME)
    }
}

private const val CLOUD_BACKUP_WORK_NAME = "cloud_backup"
private const val DEBOUNCE_DELAY_SECONDS = 10L
