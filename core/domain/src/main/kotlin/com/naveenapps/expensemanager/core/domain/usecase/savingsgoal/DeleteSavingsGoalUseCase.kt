package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.repository.AccountRepository

/**
 * Deletes a savings goal by deleting its hidden counterparty account ([SavingsGoal.account] must
 * be populated — i.e. this goal must have come from `SavingsGoalRepository.getSavingsGoals()`/
 * `findSavingsGoalById`, which always enrich it). `AccountRepositoryImpl.deleteAccount` already
 * compensates the real counterpart account's balance for every linked transfer before
 * cascade-deleting those transactions, and the `savings_goal` table's `account_id` foreign key
 * (`ON DELETE CASCADE`) removes this goal's own metadata row in the same cascade — no separate
 * `SavingsGoalRepository` delete call needed. Same shape as `DeleteDebtUseCase`.
 */
class DeleteSavingsGoalUseCase(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(savingsGoal: SavingsGoal): Resource<Boolean> {
        return accountRepository.deleteAccount(savingsGoal.account)
    }
}
