package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetPendingTransactionsUseCase(
    private val repository: PendingTransactionRepository
) {
    operator fun invoke(): Flow<List<PendingTransaction>> {
        return repository.getAllPendingTransactions()
    }
}

