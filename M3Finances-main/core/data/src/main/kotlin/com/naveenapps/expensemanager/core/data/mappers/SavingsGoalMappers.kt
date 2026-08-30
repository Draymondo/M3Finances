package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity
import com.naveenapps.expensemanager.core.model.SavingsGoal

fun SavingsGoal.toEntityModel(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = id,
        accountId = accountId,
        name = name,
        targetAmount = targetAmount,
        targetDate = targetDate,
        notes = notes,
        isAchieved = isAchieved,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun SavingsGoalEntity.toDomainModel(): SavingsGoal {
    return SavingsGoal(
        id = id,
        accountId = accountId,
        name = name,
        targetAmount = targetAmount,
        targetDate = targetDate,
        notes = notes,
        isAchieved = isAchieved,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}
