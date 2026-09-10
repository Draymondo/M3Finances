package com.naveenapps.expensemanager.core.notification.budget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val BUDGET_ALERT_WORK_NAME = "budget_alert_daily_check"

/** Same shape as `RecurringTransactionScheduler.scheduleDailyCheck` — a true periodic check
 * (once a day) so a budget that tips over 80%/100% is still flagged even on a day the person
 * never opens the app. `KEEP` means calling this on every app start doesn't reset the timer. */
class BudgetAlertScheduler(
    private val context: Context,
) {

    fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(1, TimeUnit.DAYS).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BUDGET_ALERT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
