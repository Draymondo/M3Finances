package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.AccountRepository

/**
 * Deletes an installment plan by deleting its hidden counterparty account (
 * [InstallmentPlan.account] must be populated — i.e. this plan must have come from
 * `InstallmentPlanRepository.getInstallmentPlans()`/`findInstallmentPlanById`, which always
 * enrich it). `AccountRepositoryImpl.deleteAccount` already compensates the real counterpart
 * account's balance for every linked transfer before cascade-deleting those transactions, and
 * the `installment_plan` table's `account_id` foreign key (`ON DELETE CASCADE`) removes this
 * plan's metadata row in the same cascade — no separate `InstallmentPlanRepository` delete call
 * needed. Same shape as `DeleteDebtUseCase` / `DeleteSavingsGoalUseCase`.
 */
class DeleteInstallmentPlanUseCase(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(installmentPlan: InstallmentPlan): Resource<Boolean> {
        return accountRepository.deleteAccount(installmentPlan.account)
    }
}
