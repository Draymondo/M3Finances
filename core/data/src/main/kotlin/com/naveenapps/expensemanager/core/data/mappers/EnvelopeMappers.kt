package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.EnvelopeEntity
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Envelope

fun Envelope.toEntityModel(): EnvelopeEntity {
    return EnvelopeEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        amount = amount,
        selectedMonth = selectedMonth,
        periodType = periodType.ordinal,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun EnvelopeEntity.toDomainModel(): Envelope {
    return Envelope(
        id = id,
        categoryId = categoryId,
        name = name,
        amount = amount,
        selectedMonth = selectedMonth,
        periodType = BudgetPeriod.entries.getOrElse(periodType) { BudgetPeriod.MONTHLY },
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}
