package com.naveenapps.expensemanager.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.FindDebtByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.GetDebtsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.ProcessDueRecurringTransactionsUseCase
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val notificationScheduler: NotificationScheduler,
    private val findDebtByIdUseCase: FindDebtByIdUseCase,
    private val getDebtsUseCase: GetDebtsUseCase,
    private val debtReminderRepository: DebtReminderRepository,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase,
    private val pendingTransactionRepository: com.naveenapps.expensemanager.core.repository.PendingTransactionRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Process recurring transactions and get count
        val recurringProcessedCount = try {
            processDueRecurringTransactionsUseCase.invoke()
        } catch (e: Exception) {
            0
        }

        // 2. Budget Alert Counts
        val currentBudgets = getBudgetsUseCase.invoke().first().filter { it.isCurrentPeriod() }
        val exceededBudgetsCount = currentBudgets.filter { it.percent >= 100f }.size
        val approachingBudgetsCount = currentBudgets.filter { it.percent in 80f..<100f }.size

        // 3. Overdue Debts Count
        val now = Date()
        val overdueDebtsCount = getDebtsUseCase.invoke().first().filter { debtUiModel ->
            val debt = debtUiModel.debt
            val dueDate = debt.dueDate
            !debt.isSettled && dueDate != null && dueDate.before(now)
        }.size

        // 4. Debt Reminders scheduled for today
        val allReminders = debtReminderRepository.getAllReminders().first()
        val todayRemindersCount = allReminders.filter { reminder ->
            reminder.reminderDate.isToday()
        }.filter { reminder ->
            val response = findDebtByIdUseCase.invoke(reminder.debtId)
            response is Resource.Success && !response.data.isSettled
        }.size

        // 5. Scheduled transactions due today
        val scheduledDueCount = try {
            pendingTransactionRepository.countScheduledDue()
        } catch (e: Exception) {
            0
        }

        // Build summary notification content
        val lines = mutableListOf<String>()
        if (exceededBudgetsCount > 0) {
            lines.add(context.getString(R.string.daily_summary_budget_exceeded, exceededBudgetsCount))
        }
        if (approachingBudgetsCount > 0) {
            lines.add(context.getString(R.string.daily_summary_budget_approaching, approachingBudgetsCount))
        }
        if (todayRemindersCount > 0) {
            lines.add(context.getString(R.string.daily_summary_debt_reminder, todayRemindersCount))
        }
        if (overdueDebtsCount > 0) {
            lines.add(context.getString(R.string.daily_summary_debt_overdue, overdueDebtsCount))
        }
        if (recurringProcessedCount > 0) {
            lines.add(context.getString(R.string.daily_summary_recurring_processed, recurringProcessedCount))
        }
        if (scheduledDueCount > 0) {
            lines.add(context.getString(R.string.daily_summary_scheduled_due, scheduledDueCount))
        }

        val (title, content) = if (lines.isNotEmpty()) {
            context.getString(R.string.daily_summary_title) to lines.joinToString("\n")
        } else {
            context.getString(R.string.notification_title) to context.getString(R.string.notification_description)
        }

        notificationScheduler.showNotification(
            DESTINATION_CLASS,
            title,
            content,
        )

        notificationScheduler.checkAndRestartReminder()

        return Result.success()
    }

    private fun Date.isToday(): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.time = this
        val cal2 = Calendar.getInstance()
        cal2.time = Date()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun BudgetUiModel.isCurrentPeriod(): Boolean {
        val now = Date()
        return when (periodType) {
            BudgetPeriod.YEARLY -> selectedMonth == now.toYear()
            BudgetPeriod.MONTHLY -> selectedMonth == now.toMonthAndYearKey()
            BudgetPeriod.WEEKLY -> selectedMonth == now.toWeekKey()
            BudgetPeriod.DAILY -> selectedMonth == now.toDayKey()
        }
    }
}
