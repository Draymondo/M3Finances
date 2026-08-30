package com.naveenapps.expensemanager.core.notification.debt

import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.repository.DebtRepository
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Keeps scheduled `DebtReminderWorker` jobs in sync with the `debt_reminder` table and each
 * debt's `isSettled` flag, the same "observe the data reactively" shape as
 * `DatabaseChangeCloudBackupTrigger` — chosen so that adding/removing a reminder, or marking a
 * debt settled, doesn't require every call site in the domain layer (`AddDebtReminderUseCase`,
 * `UpdateDebtUseCase`, `DeleteDebtUseCase`, ...) to remember to also touch WorkManager. Those use
 * cases live in `core:domain`, which `core:notification` depends on — not the other way around —
 * so a direct call from there isn't an option; reacting to the data changing is.
 */
class DebtReminderTrigger(
    private val debtReminderRepository: DebtReminderRepository,
    private val debtRepository: DebtRepository,
    private val debtReminderScheduler: DebtReminderScheduler,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ids scheduled as of the previous reconcile — null until the first one runs, so we never
     * cancel jobs that were already pending from a prior app session before we've seen a single
     * emission of our own. */
    private var previousScheduledIds: Set<String>? = null

    /** Call once, at app startup — see AppInitializer. */
    fun start() {
        combine(
            debtReminderRepository.getAllReminders(),
            debtRepository.getDebts(),
        ) { reminders, debts -> reminders to debts }
            .onEach { (reminders, debts) -> reconcile(reminders, debts) }
            .catch { /* A transient DB read failure shouldn't crash the app; the next emission
                         (e.g. after any write) will retry the reconcile. */ }
            .launchIn(scope)
    }

    private suspend fun reconcile(reminders: List<DebtReminder>, debts: List<Debt>) {
        val settledDebtIds = debts.filter { it.isSettled }.map { it.id }.toSet()

        val activeIds = mutableSetOf<String>()
        reminders.forEach { reminder ->
            if (reminder.debtId in settledDebtIds) return@forEach
            debtReminderScheduler.schedule(reminder)
            activeIds += reminder.id
        }

        previousScheduledIds?.minus(activeIds)?.forEach { staleId ->
            debtReminderScheduler.cancel(staleId)
        }
        previousScheduledIds = activeIds
    }
}
