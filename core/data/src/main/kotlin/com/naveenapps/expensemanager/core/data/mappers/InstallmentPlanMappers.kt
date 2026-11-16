package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.InstallmentPlanEntity
import com.naveenapps.expensemanager.core.model.InstallmentPlan

fun InstallmentPlan.toEntityModel(): InstallmentPlanEntity {
    return InstallmentPlanEntity(
        id = id,
        accountId = accountId,
        name = name,
        targetAmount = targetAmount,
        deadline = deadline,
        notes = notes,
        isCompleted = isCompleted,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun InstallmentPlanEntity.toDomainModel(): InstallmentPlan {
    return InstallmentPlan(
        id = id,
        accountId = accountId,
        name = name,
        targetAmount = targetAmount,
        deadline = deadline,
        notes = notes,
        isCompleted = isCompleted,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}
