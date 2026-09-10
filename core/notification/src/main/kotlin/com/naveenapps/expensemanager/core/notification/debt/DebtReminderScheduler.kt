package com.naveenapps.expensemanager.core.notification.debt

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.repository.ReminderTimeRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

const val KEY_DEBT_ID = "debt_id"
const val KEY_REMINDER_ID = "reminder_id"

private const val OVERDUE_CHECK_WORK_NAME = "debt_overdue_daily_check"

/**
 * One-time WorkManager job per [DebtReminder] — deliberately *not* the shared `NotificationScheduler`
 * (that class owns a single "daily_reminder" unique work slot; a debt can have any number of its
 * own reminder dates, so each needs its own uniquely-named job) — plus a separate daily periodic
 * check that keeps nagging about any overdue, unsettled debt regardless of whether the person set
 * a specific reminder date for it.
 *
 * A reminder fires at the person's configured daily-reminder time-of-day (see
 * `ReminderTimeRepository`) on the chosen date — reused purely for a consistent notification
 * hour, independent of whether that general daily reminder toggle is itself on or off (see
 * `canShowDebtNotification`).
 */
class DebtReminderScheduler(
    private val context: Context,
    private val reminderTimeRepository: ReminderTimeRepository,
) {

    suspend fun schedule(reminder: DebtReminder) {
        val delay = (targetTimeMillis(reminder.reminderDate) - Date().time).coerceAtLeast(0L)

        val data = Data.Builder()
            .putString(KEY_DEBT_ID, reminder.debtId)
            .putString(KEY_REMINDER_ID, reminder.id)
            .build()

        val request = OneTimeWorkRequestBuilder<DebtReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(reminder.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(reminderId))
    }

    /** Call once, at app startup — idempotent (`KEEP`), same pattern as
     * `RecurringTransactionScheduler.scheduleDailyCheck`. */
    fun scheduleOverdueDailyCheck() {
        val request = PeriodicWorkRequestBuilder<DebtOverdueWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OVERDUE_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private suspend fun targetTimeMillis(reminderDate: Date): Long {
        val reminderTime = reminderTimeRepository.getReminderTime().first()
        val calendar = Calendar.getInstance()
        calendar.time = reminderDate
        calendar.set(Calendar.HOUR_OF_DAY, reminderTime.hour)
        calendar.set(Calendar.MINUTE, reminderTime.minute)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    private fun workName(reminderId: String) = "debt_reminder_$reminderId"
}
