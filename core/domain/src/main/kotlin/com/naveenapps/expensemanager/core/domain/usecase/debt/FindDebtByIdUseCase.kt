package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtRepository

class FindDebtByIdUseCase(
    private val repository: DebtRepository,
) {
    suspend operator fun invoke(id: String): Resource<Debt> {
        return repository.findDebtById(id)
    }
}
