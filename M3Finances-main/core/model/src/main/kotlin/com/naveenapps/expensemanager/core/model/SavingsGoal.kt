package com.naveenapps.expensemanager.core.model

import androidx.compose.runtime.Stable
import java.util.Date

/**
 * Metadata for a savings goal. Same design as [Debt]: contributions (and withdrawals) are
 * recorded as ordinary [TransactionType.TRANSFER] transactions between a real account and this
 * goal's hidden [AccountType.SAVINGS_GOAL] counterparty account (see [accountId]), reusing the
 * app's existing atomic balance mechanism instead of a parallel ledger — so the amount saved so
 * far is always just that account's current balance, never stored here redundantly.
 *
 * Doing it this way (rather than just pointing a goal at one of the user's existing accounts and
 * reading its balance directly) is what lets several goals share the same real account as their
 * funding source — e.g. a vacation goal and an emergency fund both fed from the same checking
 * account — without the two amounts getting mixed together.
 *
 * [isAchieved] is a manual override, same reasoning as [Debt.isSettled]: lets the person close a
 * goal even when the saved amount isn't exactly at (or has gone slightly over/under) the target.
 */
@Stable
data class SavingsGoal(
    val id: String,
    /** The hidden [AccountType.SAVINGS_GOAL] account backing this goal — its balance is the
     * amount saved so far. */
    val accountId: String,
    val name: String,
    val targetAmount: Double,
    val targetDate: Date?,
    val notes: String,
    val isAchieved: Boolean,
    val createdOn: Date,
    val updatedOn: Date,
    /** Populated for display by the domain layer; empty/default until then. */
    val account: Account = Account(
        id = "",
        name = "",
        type = AccountType.SAVINGS_GOAL,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        amount = 0.0,
        creditLimit = 0.0,
        createdOn = Date(),
        updatedOn = Date(),
    ),
)

/** Progress toward [SavingsGoal.targetAmount], clamped to `[0, 1]` for direct use in a progress
 * bar — the saved amount can technically overshoot the target (e.g. a late contribution rounded
 * up), which this caps rather than propagating into an over-full bar. */
fun SavingsGoal.progress(): Float {
    if (targetAmount <= 0.0) return 0f
    return (account.amount / targetAmount).toFloat().coerceIn(0f, 1f)
}
