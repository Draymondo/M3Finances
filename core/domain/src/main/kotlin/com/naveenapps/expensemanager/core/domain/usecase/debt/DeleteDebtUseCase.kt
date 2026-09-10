package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.AccountRepository

/**
 * Deletes a debt by deleting its hidden counterparty account ([Debt.account] must be populated —
 * i.e. this debt must have come from `DebtRepository.getDebts()`/`findDebtById`, which always
 * enrich it). `AccountRepositoryImpl.deleteAccount` already compensates the real counterpart
 * account's balance for every linked transfer before cascade-deleting those transactions, and
 * the `debt` table's `account_id` foreign key (`ON DELETE CASCADE`) removes this debt's own
 * metadata row in the same cascade — no separate `DebtRepository` delete call needed.
 */
class DeleteDebtUseCase(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(debt: Debt): Resource<Boolean> {
        return accountRepository.deleteAccount(debt.account)
    }
}
