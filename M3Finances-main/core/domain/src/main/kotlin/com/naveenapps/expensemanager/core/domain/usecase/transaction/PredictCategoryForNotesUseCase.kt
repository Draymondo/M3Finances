package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.TransactionRepository

class PredictCategoryForNotesUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(notes: String, transactionType: TransactionType): String? {
        if (notes.isBlank()) return null
        return transactionRepository.getMostFrequentCategoryIdForNotes(notes, transactionType)
    }
}

