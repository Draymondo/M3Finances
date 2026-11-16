package com.naveenapps.expensemanager.core.model

import androidx.compose.runtime.Stable
import java.util.Date

/**
 * Metadata for an installment purchase — something the user is paying for (or repaying) in
 * several payments over time, e.g. a phone bought on store credit, school fees paid in
 * installments, or a service settled over multiple transfers.
 *
 * Same design as [Debt] / [SavingsGoal]: every payment (and, if ever needed, a refund) is
 * recorded as an ordinary [TransactionType.TRANSFER] transaction between a real account and this
 * plan's hidden [AccountType.INSTALLMENT] counterparty account (see [accountId]), reusing the
 * app's existing atomic balance mechanism instead of a parallel ledger — so the amount paid so
 * far is always just that account's current balance, never stored here redundantly.
 *
 * Payments are free-form (no fixed schedule is imposed): the user decides how much and when to
 * pay, and tracks the countdown to [deadline] themselves. [deadline] is only used for display
 * (how many days/months remain, or how late the plan is), never to generate anything.
 *
 * [isCompleted] is a manual override to close a plan even when the hidden account's balance
 * isn't exactly at (or has gone slightly over/under) [targetAmount] — same reasoning as
 * [Debt.isSettled] / [SavingsGoal.isAchieved].
 */
@Stable
data class InstallmentPlan(
    val id: String,
    /** The hidden [AccountType.INSTALLMENT] account backing this plan — its balance is the
     * amount paid so far. */
    val accountId: String,
    /** What is being paid for — e.g. "Téléphone Samsung", "Frais de scolarité". */
    val name: String,
    /** Final price to reach — the amount the user set as the goal of the plan. */
    val targetAmount: Double,
    /** Optional limit date — display-only (days/months remaining), null = no deadline. */
    val deadline: Date?,
    val notes: String,
    val isCompleted: Boolean,
    val createdOn: Date,
    val updatedOn: Date,
    /** Populated for display by the domain layer; empty/default until then. */
    val account: Account = Account(
        id = "",
        name = "",
        type = AccountType.INSTALLMENT,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        amount = 0.0,
        creditLimit = 0.0,
        createdOn = Date(),
        updatedOn = Date(),
    ),
)

/** Progress toward [InstallmentPlan.targetAmount], clamped to `[0, 1]` for direct use in a
 * progress bar — the paid amount can technically overshoot the target, which this caps rather
 * than propagating into an over-full bar. */
fun InstallmentPlan.progress(): Float {
    if (targetAmount <= 0.0) return 0f
    return (account.amount / targetAmount).toFloat().coerceIn(0f, 1f)
}
