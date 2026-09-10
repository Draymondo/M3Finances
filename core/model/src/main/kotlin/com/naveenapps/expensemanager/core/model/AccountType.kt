package com.naveenapps.expensemanager.core.model

/**
 * IMPORTANT: this enum is persisted by ordinal (see `AccountTypeConverter`), so new entries
 * must always be appended at the end — inserting in the middle would silently reinterpret
 * every existing account's stored type.
 */
enum class AccountType {
    REGULAR,
    CREDIT,
    MOBILE_MONEY,
    /**
     * Hidden counterparty account created automatically for each [Debt] — represents "how much
     * X owes me" or "how much I owe X". Never shown in the normal account list, the transaction
     * account picker, or the dashboard's net balance; only reachable through the debt feature
     * itself. See `Debt` for the full design.
     */
    DEBT,

    /**
     * Hidden account created automatically for each [SavingsGoal] — accumulates contributions
     * transferred in from a real account, same "hidden counterparty" shape as [DEBT] above (see
     * `SavingsGoal` for the full design), including the same tradeoff: contributing to a goal
     * moves money out of the visible net balance, same as lending money out does today. Never
     * shown in the normal account list, the transaction account picker, or the dashboard's net
     * balance; only reachable through the savings goal feature itself.
     */
    SAVINGS_GOAL,
    
    /**
     * Account type for "Coffre" (Vault/Safe)
     */
    VAULT
}

fun AccountType.isRegular() = this == AccountType.REGULAR

fun AccountType.isCredit() = this == AccountType.CREDIT

fun AccountType.isMobileMoney() = this == AccountType.MOBILE_MONEY

fun AccountType.isDebt() = this == AccountType.DEBT

fun AccountType.isSavingsGoal() = this == AccountType.SAVINGS_GOAL

fun AccountType.isVault() = this == AccountType.VAULT
