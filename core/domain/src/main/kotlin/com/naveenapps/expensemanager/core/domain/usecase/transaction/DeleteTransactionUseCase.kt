package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
) {

    suspend operator fun invoke(transaction: Transaction): Resource<Boolean> {
        if (transaction.id.isBlank()) {
            return Resource.Error(Exception("L'identifiant ne doit pas être vide"))
        }

        if (transaction.categoryId.isBlank()) {
            return Resource.Error(Exception("Le type de catégorie ne doit pas être vide"))
        }

        if (transaction.fromAccountId.isBlank()) {
            return Resource.Error(Exception("Le compte source ne doit pas être vide"))
        }

        return repository.deleteTransaction(transaction)
    }
}
