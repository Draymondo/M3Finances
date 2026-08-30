package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import java.math.BigDecimal

internal fun validateAndNormalizeSplit(transaction: Transaction): Resource<Transaction> {
    if (transaction.splitItems.size < 2) {
        return Resource.Success(transaction.copy(splitItems = emptyList()))
    }

    if (transaction.splitItems.any { it.categoryId.isBlank() || it.amount.amount <= 0.0 }) {
        return Resource.Error(IllegalArgumentException("Split items must have a category and a positive amount"))
    }

    val splitTotal = transaction.splitItems
        .map { BigDecimal.valueOf(it.amount.amount) }
        .reduce(BigDecimal::add)
    val transactionTotal = BigDecimal.valueOf(transaction.amount.amount)
    if (splitTotal.compareTo(transactionTotal) != 0) {
        return Resource.Error(IllegalArgumentException("Split items must equal the transaction amount"))
    }

    val defaultCategoryId = transaction.splitItems.maxBy { it.amount.amount }.categoryId
    return Resource.Success(
        transaction.copy(
            categoryId = defaultCategoryId,
            splitItems = transaction.splitItems,
        )
    )
}
