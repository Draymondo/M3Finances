package com.naveenapps.expensemanager.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.naveenapps.expensemanager.core.data.cloudbackup.DatabaseChangeCloudBackupTrigger
import com.naveenapps.expensemanager.core.notification.recurringtransaction.RecurringTransactionScheduler
import com.naveenapps.expensemanager.core.notification.debt.DebtReminderScheduler
import com.naveenapps.expensemanager.core.notification.debt.DebtReminderTrigger
import com.naveenapps.expensemanager.core.notification.budget.BudgetAlertScheduler
import com.naveenapps.expensemanager.core.database.DATABASE_FILE_NAME
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.ProcessDueRecurringTransactionsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.locale.ApplyLocaleUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.theme.ApplyThemeUseCase
import com.naveenapps.expensemanager.core.model.CloudSyncOutcome
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.notification.NotificationScheduler
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class AppInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        val applyThemeUseCase: ApplyThemeUseCase = GlobalContext.get().get()
        val applyLocaleUseCase: ApplyLocaleUseCase = GlobalContext.get().get()
        val notificationScheduler: NotificationScheduler = GlobalContext.get().get()
        val googleAuthRepository: GoogleAuthRepository = GlobalContext.get().get()
        val cloudBackupRepository: CloudBackupRepository = GlobalContext.get().get()
        val databaseChangeCloudBackupTrigger: DatabaseChangeCloudBackupTrigger =
            GlobalContext.get().get()
        val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase =
            GlobalContext.get().get()
        val recurringTransactionScheduler: RecurringTransactionScheduler =
            GlobalContext.get().get()
        val debtReminderTrigger: DebtReminderTrigger = GlobalContext.get().get()
        val debtReminderScheduler: DebtReminderScheduler = GlobalContext.get().get()
        val budgetAlertScheduler: BudgetAlertScheduler = GlobalContext.get().get()

        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            // Capture this *before* any restore: a successful fresh-install restore creates the
            // Room file, and we must not then treat that as "existing local data" and push it.
            val databaseAlreadyExisted = context.getDatabasePath(DATABASE_FILE_NAME).exists()

            // Must run before anything below (or any UI screen) queries the Room database
            // (ApplyThemeUseCase / ApplyLocaleUseCase only touch DataStore, not Room, so this
            // ordering is safe) — restoring writes rows straight into Room via the DAOs, and
            // doing that as early as possible avoids any screen reading a still-empty database
            // in the brief window before the restore finishes. See
            // CloudBackupRepository.restoreAllFromCloud.
            if (databaseAlreadyExisted) {
                reconcileExistingLocalDatabase(googleAuthRepository, cloudBackupRepository)
            } else {
                restoreFromCloudIfFreshInstall(googleAuthRepository, cloudBackupRepository)
            }

            applyThemeUseCase.invoke()
            applyLocaleUseCase.invoke()
            notificationScheduler.checkAndRestartReminder()

            // Safe to start listening for local changes only now that any pending restore (if
            // any) has already happened — otherwise a restore's own file replacement could be
            // mistaken for a local edit and immediately re-uploaded.
            databaseChangeCloudBackupTrigger.start()

            // Catch up on anything due right away (e.g. rent that came due while the app was
            // closed) rather than waiting for the first scheduled daily check; scheduleDailyCheck
            // then keeps it running even on days the person never opens the app (see
            // RecurringTransactionScheduler).
            processDueRecurringTransactionsUseCase.invoke()
            recurringTransactionScheduler.scheduleDailyCheck()

            // Reconciles WorkManager jobs against the current debt_reminder table (see
            // DebtReminderTrigger) and keeps the overdue-debt daily check running, same pattern
            // as the recurring-transaction scheduler above.
            debtReminderTrigger.start()
            debtReminderScheduler.scheduleOverdueDailyCheck()

            // Same daily-check pattern as the two schedulers above — see BudgetAlertWorker.
            budgetAlertScheduler.scheduleDailyCheck()
        }
    }

    /**
     * Only ever attempts a fully silent sign-in (see GoogleAuthRepository.trySilentSignIn) —
     * never shows any UI at cold app start. On a genuinely fresh install where the person hasn't
     * connected a Google account with this app before (e.g. their very first install ever, or a
     * new device where they haven't done the one-time explicit sign-in yet), this simply does
     * nothing and the app proceeds exactly as it always has, with a fresh empty local database.
     */
    private suspend fun restoreFromCloudIfFreshInstall(
        googleAuthRepository: GoogleAuthRepository,
        cloudBackupRepository: CloudBackupRepository,
    ) {
        val signInResult = googleAuthRepository.trySilentSignIn()
        if (signInResult !is Resource.Success) return

        val hasRemoteBackupResult = cloudBackupRepository.hasRemoteBackup()
        if (hasRemoteBackupResult !is Resource.Success || !hasRemoteBackupResult.data) return

        when (val restoreResult = cloudBackupRepository.restoreAllFromCloud()) {
            is Resource.Success -> Log.i(TAG, "Cloud backup restored on fresh install")
            is Resource.Error -> Log.w(TAG, "Cloud backup restore failed", restoreResult.exception)
        }
    }

    /**
     * Room already exists, so the old "only restore if the file is missing" path would skip
     * entirely and the next local edit (or the Room trigger) could push a stale phone copy over
     * a newer cloud copy. [CloudBackupRepository.syncAll] restores when the cloud is newer and
     * this device has no local edits, or raises [CloudSyncOutcome.Conflict] instead of writing.
     */
    private suspend fun reconcileExistingLocalDatabase(
        googleAuthRepository: GoogleAuthRepository,
        cloudBackupRepository: CloudBackupRepository,
    ) {
        val signInResult = googleAuthRepository.trySilentSignIn()
        if (signInResult !is Resource.Success) return
        if (googleAuthRepository.getCurrentUserId() == null) return

        when (val result = cloudBackupRepository.syncAll()) {
            is Resource.Success -> when (result.data) {
                CloudSyncOutcome.Restored -> Log.i(TAG, "Cloud copy restored over stale local database")
                CloudSyncOutcome.Conflict -> Log.w(TAG, "Cloud sync conflict — waiting for user choice")
                CloudSyncOutcome.Pushed -> Log.i(TAG, "Local changes pushed to cloud at startup")
                CloudSyncOutcome.NoOp -> Unit
            }
            is Resource.Error -> Log.w(TAG, "Startup cloud reconcile failed", result.exception)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(
            KoinInitializer::class.java,
            WorkManagerInitializer::class.java
        )
    }

    companion object {
        private const val TAG = "AppInitializer"
    }
}
