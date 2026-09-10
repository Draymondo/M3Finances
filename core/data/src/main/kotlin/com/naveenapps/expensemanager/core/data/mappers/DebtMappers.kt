package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.DebtEntity
import com.naveenapps.expensemanager.core.database.entity.DebtReminderEntity
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.DebtReminder

fun Debt.toEntityModel(): DebtEntity {
    return DebtEntity(
        id = id,
        accountId = accountId,
        personName = personName,
        direction = direction,
        dueDate = dueDate,
        notes = notes,
        isSettled = isSettled,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun DebtEntity.toDomainModel(): Debt {
    return Debt(
        id = id,
        accountId = accountId,
        personName = personName,
        direction = direction,
        dueDate = dueDate,
        notes = notes,
        isSettled = isSettled,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun DebtReminder.toEntityModel(): DebtReminderEntity {
    return DebtReminderEntity(
        id = id,
        debtId = debtId,
        reminderDate = reminderDate,
        createdOn = createdOn,
    )
}

fun DebtReminderEntity.toDomainModel(): DebtReminder {
    return DebtReminder(
        id = id,
        debtId = debtId,
        reminderDate = reminderDate,
        createdOn = createdOn,
    )
}
