package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository

class FindRecurringTransactionByIdUseCase(
    private val repository: RecurringTransactionRepository,
) {

    suspend operator fun invoke(id: String): Resource<RecurringTransaction> {
        return repository.findRecurringTransactionById(id)
    }
}
