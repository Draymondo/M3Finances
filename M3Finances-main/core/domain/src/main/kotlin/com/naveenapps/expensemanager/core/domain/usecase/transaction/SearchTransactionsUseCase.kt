package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class SearchTransactionsUseCase(
    private val repository: TransactionRepository,
) {
    operator fun invoke(query: String): Flow<List<Transaction>?> =
        repository.searchTransactions(query)
}