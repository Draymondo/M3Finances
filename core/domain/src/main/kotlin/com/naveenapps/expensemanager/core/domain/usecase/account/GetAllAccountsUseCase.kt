package com.naveenapps.expensemanager.core.domain.usecase.account

import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.isDebt
import com.naveenapps.expensemanager.core.model.isSavingsGoal
import com.naveenapps.expensemanager.core.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Every current caller (account list, account reorder, the shared account picker used by
 * transactions/budgets/recurring transactions, the dashboard) represents "normal" account usage,
 * so [AccountType.DEBT] and [AccountType.SAVINGS_GOAL] accounts — hidden counterparty accounts
 * created by the debt and savings-goal features — are filtered out here rather than in each
 * consumer. Code that specifically needs one of those accounts (the debt/savings-goal features
 * themselves) should go through their own repository/`AccountRepository` directly instead of
 * this use case.
 */
class GetAllAccountsUseCase(private val repository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> {
        return repository.getAccounts().map { accounts ->
            accounts.filterNot { it.type.isDebt() || it.type.isSavingsGoal() }
        }
    }
}
