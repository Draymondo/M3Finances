package com.naveenapps.expensemanager.core.model

import java.util.Date

/**
 * A template that periodically creates a real [Transaction] — rent, salary, subscriptions, and
 * so on. Reuses the existing transaction creation path (see
 * `ProcessDueRecurringTransactionsUseCase` / `AddTransactionUseCase`) so account balances update
 * through the same atomic mechanism as any manually-entered transaction; this model only adds
 * the scheduling metadata on top.
 */
data class RecurringTransaction(
    val id: String,
    val notes: String,
    val categoryId: String,
    val fromAccountId: String,
    val toAccountId: String?,
    val amount: Amount,
    val type: TransactionType,
    val frequency: RecurrenceFrequency,
    /** Fire every N [frequency] periods — e.g. frequency=WEEKLY, interval=2 means fortnightly. */
    val interval: Int = 1,
    val startDate: Date,
    /** Null means "repeats indefinitely". */
    val endDate: Date? = null,
    /** The next date a real transaction should be created for this template; advanced by
     * [interval] x [frequency] each time it fires. */
    val nextOccurrenceDate: Date,
    /** Paused templates are kept (not deleted) but never processed — lets the person temporarily
     * suspend e.g. a subscription without losing the schedule. */
    val isActive: Boolean = true,
    val createdOn: Date,
    val updatedOn: Date,
    var category: Category = Category(
        "",
        "",
        CategoryType.INCOME,
        StoredIcon("", ""),
        Date(),
        Date(),
    ),
    var fromAccount: Account = Account(
        "",
        "",
        AccountType.REGULAR,
        StoredIcon("", ""),
        Date(),
        Date(),
    ),
    var toAccount: Account? = null,
)
