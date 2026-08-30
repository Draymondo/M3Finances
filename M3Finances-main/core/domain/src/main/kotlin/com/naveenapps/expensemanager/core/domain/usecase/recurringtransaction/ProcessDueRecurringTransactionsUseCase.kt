package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * Runs periodically in the background (see the recurring-transaction WorkManager worker) and
 * turns each due [RecurringTransaction] template into a real [Transaction] — reusing
 * [AddTransactionUseCase] so account balances update through the exact same validated path as a
 * manually-entered transaction.
 *
 * If the app hasn't been opened in a while, a template can be overdue by more than one occurrence
 * (e.g. a weekly template with the app untouched for a month) — this catches up by creating one
 * transaction per missed occurrence rather than silently skipping to the next future date, since
 * each missed occurrence generally represents money that was actually spent/received regardless
 * of whether the app was open to record it. [MAX_CATCH_UP_OCCURRENCES] is a safety cap against a
 * runaway loop if a template's dates were ever corrupted.
 */
class ProcessDueRecurringTransactionsUseCase(
    private val repository: RecurringTransactionRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
) {

    suspend operator fun invoke(): Int {
        var totalProcessed = 0
        repository.getDueRecurringTransactions().forEach { template ->
            totalProcessed += processTemplate(template)
        }
        return totalProcessed
    }

    private suspend fun processTemplate(template: RecurringTransaction): Int {
        var occurrenceDate = template.nextOccurrenceDate
        val now = Date()
        var occurrencesProcessed = 0

        while (!occurrenceDate.after(now) && occurrencesProcessed < MAX_CATCH_UP_OCCURRENCES) {
            if (template.endDate != null && occurrenceDate.after(template.endDate)) {
                break
            }

            addTransactionUseCase.invoke(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    notes = template.notes,
                    categoryId = template.categoryId,
                    fromAccountId = template.fromAccountId,
                    toAccountId = template.toAccountId,
                    type = template.type,
                    amount = Amount(template.amount.amount),
                    imagePath = "",
                    createdOn = occurrenceDate,
                    updatedOn = occurrenceDate,
                ),
            )

            occurrenceDate = advance(occurrenceDate, template.frequency, template.interval)
            occurrencesProcessed++
        }

        repository.advanceToNextOccurrence(template.id, occurrenceDate)
        return occurrencesProcessed
    }

    private fun advance(from: Date, frequency: RecurrenceFrequency, interval: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = from
        val field = when (frequency) {
            RecurrenceFrequency.DAILY -> Calendar.DAY_OF_MONTH
            RecurrenceFrequency.WEEKLY -> Calendar.WEEK_OF_YEAR
            RecurrenceFrequency.MONTHLY -> Calendar.MONTH
            RecurrenceFrequency.YEARLY -> Calendar.YEAR
        }
        calendar.add(field, interval.coerceAtLeast(1))
        return calendar.time
    }

    companion object {
        private const val MAX_CATCH_UP_OCCURRENCES = 366
    }
}
