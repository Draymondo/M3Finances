package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction

class UpdateTransactionUseCase(
    private val repository: com.naveenapps.expensemanager.core.repository.TransactionRepository,
) {

    suspend operator fun invoke(transaction: Transaction): Resource<Boolean> {
        if (transaction.id.isBlank()) {
            return Resource.Error(Exception("ID shouldn't be blank"))
        }

        if (transaction.categoryId.isBlank()) {
            return Resource.Error(Exception("Category shouldn't be blank"))
        }

        if (transaction.amount.amount <= 0.0) {
            return Resource.Error(Exception("Amount should be greater than 0"))
        }

        return when (val validation = validateAndNormalizeSplit(transaction)) {
            is Resource.Error -> validation
            is Resource.Success -> repository.updateTransaction(validation.data)
        }
    }
}
