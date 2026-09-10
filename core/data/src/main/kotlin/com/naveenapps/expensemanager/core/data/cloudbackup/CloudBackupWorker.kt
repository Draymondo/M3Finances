package com.naveenapps.expensemanager.core.data.cloudbackup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.model.CloudSyncOutcome
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository

class CloudBackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val googleAuthRepository: GoogleAuthRepository,
    private val cloudBackupRepository: CloudBackupRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (CloudBackupOperationState.isRestoreInProgress()) {
            return Result.success()
        }

        // Nothing to do if no Google identity has been established yet — the person simply
        // hasn't gone through sign-in, which is a normal, expected state, not a failure to
        // retry.
        if (googleAuthRepository.getCurrentUserId() == null) {
            return Result.success()
        }

        return when (val result = cloudBackupRepository.syncAll()) {
            is Resource.Success -> {
                when (result.data) {
                    CloudSyncOutcome.Pushed -> {
                        when (cloudBackupRepository.createDailySnapshotIfNeeded()) {
                            is Resource.Success -> Result.success()
                            is Resource.Error -> Result.retry()
                        }
                    }
                    CloudSyncOutcome.Restored,
                    CloudSyncOutcome.NoOp -> {
                        when (cloudBackupRepository.createDailySnapshotIfNeeded()) {
                            is Resource.Success -> Result.success()
                            is Resource.Error -> Result.retry()
                        }
                    }
                    CloudSyncOutcome.Conflict -> Result.success()
                }
            }
            is Resource.Error -> Result.retry()
        }
    }
}
