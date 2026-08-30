package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.TransactionEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionSplitItemEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionSplitItemRelation
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionSplitItem

fun Transaction.toEntityModel(): TransactionEntity {
    return TransactionEntity(
        id = id,
        notes = notes,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        type = type,
        amount = amount.amount,
        imagePath = imagePath,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun TransactionSplitItem.toEntityModel(): TransactionSplitItemEntity = TransactionSplitItemEntity(
    id = id,
    transactionId = transactionId,
    categoryId = categoryId,
    amount = amount.amount,
    notes = notes,
)

fun TransactionSplitItemRelation.toDomainModel(): TransactionSplitItem = TransactionSplitItem(
    id = splitItemEntity.id,
    transactionId = splitItemEntity.transactionId,
    categoryId = splitItemEntity.categoryId,
    amount = Amount(splitItemEntity.amount),
    notes = splitItemEntity.notes,
    category = categoryEntity?.toDomainModel(),
)

fun TransactionEntity.toDomainModel(): Transaction {
    return Transaction(
        id = id,
        notes = notes,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        type = type,
        amount = Amount(amount),
        imagePath = imagePath,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}
