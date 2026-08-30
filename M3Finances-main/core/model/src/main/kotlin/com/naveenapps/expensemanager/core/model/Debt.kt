package com.naveenapps.expensemanager.core.model

import androidx.compose.runtime.Stable
import java.util.Date

/**
 * IMPORTANT: persisted by ordinal (see `DebtDirectionConverter`), so new entries must always be
 * appended at the end — same caveat as `AccountType`.
 */
enum class DebtDirection {
    /** The person owes money to the user. */
    LENT,

    /** The user owes money to the person. */
    BORROWED,
}

/**
 * Metadata for a debt with a person. The actual money movement — lending, borrowing, and every
 * partial repayment — is recorded as ordinary [TransactionType.TRANSFER] transactions between a
 * real account and this debt's hidden [AccountType.DEBT] counterparty account (see [accountId]).
 * That reuses the app's existing atomic balance mechanism instead of a parallel ledger, so the
 * remaining amount owed is always just that account's current balance — never stored here
 * redundantly.
 *
 * [isSettled] is a manual override to close a debt even when the account balance isn't exactly
 * zero (e.g. the person repaid in cash, outside the app, and the user doesn't want to bother
 * recording the exact matching transaction).
 */
@Stable
data class Debt(
    val id: String,
    /** The hidden [AccountType.DEBT] account backing this debt — its balance is the amount owed. */
    val accountId: String,
    /** Free text; no contacts integration. */
    val personName: String,
    val direction: DebtDirection,
    val dueDate: Date?,
    val notes: String,
    val isSettled: Boolean,
    val createdOn: Date,
    val updatedOn: Date,
    /** Populated for display by the domain layer; empty/default until then. */
    val account: Account = Account(
        id = "",
        name = "",
        type = AccountType.DEBT,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        amount = 0.0,
        creditLimit = 0.0,
        createdOn = Date(),
        updatedOn = Date(),
    ),
)

fun DebtDirection.isLent() = this == DebtDirection.LENT

fun DebtDirection.isBorrowed() = this == DebtDirection.BORROWED

/**
 * A single user-chosen reminder date for a [Debt] — see `DebtReminderEntity` for why these are
 * freely picked by the person rather than derived automatically from [Debt.dueDate].
 */
@Stable
data class DebtReminder(
    val id: String,
    val debtId: String,
    val reminderDate: Date,
    val createdOn: Date,
)
