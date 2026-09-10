package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.RecurringTransaction

fun RecurringTransaction.toEntityModel(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = id,
        notes = notes,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        type = type,
        amount = amount.amount,
        frequency = frequency,
        intervalCount = interval,
        startDate = startDate,
        endDate = endDate,
        nextOccurrenceDate = nextOccurrenceDate,
        isActive = isActive,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun RecurringTransactionEntity.toDomainModel(): RecurringTransaction {
    return RecurringTransaction(
        id = id,
        notes = notes,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        type = type,
        amount = Amount(amount),
        frequency = frequency,
        interval = intervalCount,
        startDate = startDate,
        endDate = endDate,
        nextOccurrenceDate = nextOccurrenceDate,
        isActive = isActive,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}
