package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtRepository

/** Updates a debt's metadata (person name, due date, notes, or the manual [Debt.isSettled] flag).
 * Doesn't touch the hidden account or any transaction — use [AddDebtRepaymentUseCase] for that. */
class UpdateDebtUseCase(
    private val repository: DebtRepository,
) {
    suspend operator fun invoke(debt: Debt): Resource<Boolean> {
        if (debt.personName.isBlank()) {
            return Resource.Error(Exception("Le nom de la personne ne doit pas être vide"))
        }

        return repository.updateDebt(debt)
    }
}
