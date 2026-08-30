package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository

class DeleteDebtReminderUseCase(
    private val repository: DebtReminderRepository,
) {
    suspend operator fun invoke(id: String): Resource<Boolean> {
        return repository.deleteReminder(id)
    }
}
