package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetAllRecurringTransactionUseCase(
    private val repository: RecurringTransactionRepository,
) {

    operator fun invoke(): Flow<List<RecurringTransaction>> {
        return repository.getAllRecurringTransactions()
    }
}
