package com.naveenapps.expensemanager.core.notification.debt

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.notification.R
import com.naveenapps.expensemanager.core.repository.DebtRepository
import kotlinx.coroutines.flow.first
import java.util.Date

/** Fixed notification id — this is one grouped summary notification (not one per debt), so a
 * person with several overdue debts doesn't get spammed with a stack of separate notifications
 * every day. Tapping it just opens the app; see `DebtListScreen` for the actual list.
 *
 * Not `const` — `String.hashCode()` isn't a compile-time constant in Kotlin, so `const val`
 * doesn't compile here even though the value is effectively fixed at runtime. */
private val OVERDUE_NOTIFICATION_REQUEST_CODE = "debt_overdue_summary".hashCode()

class DebtOverdueWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val debtRepository: DebtRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return Result.success()
    }
}
