package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository

class UpdateRecurringTransactionUseCase(
    private val repository: RecurringTransactionRepository,
) {

    suspend operator fun invoke(recurringTransaction: RecurringTransaction): Resource<Boolean> {
        if (recurringTransaction.categoryId.isBlank()) {
            return Resource.Error(Exception("La catégorie ne doit pas être vide"))
        }

        if (recurringTransaction.amount.amount <= 0.0) {
            return Resource.Error(Exception("Le montant doit être supérieur à 0"))
        }

        return repository.updateRecurringTransaction(recurringTransaction)
    }
}
