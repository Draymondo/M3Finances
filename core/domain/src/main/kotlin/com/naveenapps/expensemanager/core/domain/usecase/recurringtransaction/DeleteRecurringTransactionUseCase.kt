package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository

class DeleteRecurringTransactionUseCase(
    private val repository: RecurringTransactionRepository,
) {

    suspend operator fun invoke(recurringTransaction: RecurringTransaction): Resource<Boolean> {
        return repository.deleteRecurringTransaction(recurringTransaction)
    }
}
