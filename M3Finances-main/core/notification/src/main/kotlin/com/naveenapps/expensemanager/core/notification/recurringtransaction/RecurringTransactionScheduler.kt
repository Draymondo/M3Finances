package com.naveenapps.expensemanager.core.notification.recurringtransaction

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Unlike the debounced cloud backup, this is a true periodic background check (once a day) —
 * recurring transactions need to fire even if the person never opens the app on the day they're
 * due (e.g. rent due the 1st while the person is traveling). [start] also runs the check
 * immediately via [com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.ProcessDueRecurringTransactionsUseCase]
 * at app startup — see AppInitializer — so due templates aren't left waiting for the next
 * scheduled run.
 *
 * `KEEP` (rather than `REPLACE`) means calling this on every app start doesn't reset the
 * once-a-day timer each time — it only actually schedules on the very first call.
 */
class RecurringTransactionScheduler(
    private val context: Context,
) {

    fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                RECURRING_TRANSACTION_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }
}

private const val RECURRING_TRANSACTION_WORK_NAME = "recurring_transaction_daily_check"
