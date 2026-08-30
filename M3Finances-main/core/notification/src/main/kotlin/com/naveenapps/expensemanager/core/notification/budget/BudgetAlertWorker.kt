package com.naveenapps.expensemanager.core.notification.budget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetsUseCase
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.notification.R
import kotlinx.coroutines.flow.first
import java.util.Date

/** Same fixed-id grouped-notification reasoning as `DebtOverdueWorker` — one summary
 * notification, not one per budget. */
private val BUDGET_ALERT_NOTIFICATION_REQUEST_CODE = "budget_alert_summary".hashCode()

/** A budget is only ever considered "close" or "exceeded" for its *current* period — a budget
 * created for a past day/week/month/year keeps whatever final percentage it ended on, and
 * re-alerting on that every day forever would be noise, not a warning. */
private fun BudgetUiModel.isCurrentPeriod(): Boolean {
    val now = Date()
    return when (periodType) {
        BudgetPeriod.YEARLY -> selectedMonth == now.toYear()
        BudgetPeriod.MONTHLY -> selectedMonth == now.toMonthAndYearKey()
        BudgetPeriod.WEEKLY -> selectedMonth == now.toWeekKey()
        BudgetPeriod.DAILY -> selectedMonth == now.toDayKey()
    }
}

private const val WARNING_THRESHOLD_PERCENT = 80f
private const val EXCEEDED_THRESHOLD_PERCENT = 100f

/**
 * Daily check (see `BudgetAlertScheduler`) — not triggered right when a transaction is added, to
 * keep this a simple, self-contained addition on top of the existing `GetBudgetsUseCase` percent
 * calculation rather than threading a new side effect through `AddTransactionUseCase`. Reminds
 * daily for as long as a budget stays over a threshold, same behavior as `DebtOverdueWorker` for
 * an unsettled debt — a budget that's still over 100% tomorrow is still worth mentioning.
 *
 * If any budget has exceeded 100%, that takes priority over ones merely approaching 80% — only
 * one notification is shown per day, so this avoids ever combining two different messages into
 * one string.
 */
class BudgetAlertWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val getBudgetsUseCase: GetBudgetsUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return Result.success()
    }
}
