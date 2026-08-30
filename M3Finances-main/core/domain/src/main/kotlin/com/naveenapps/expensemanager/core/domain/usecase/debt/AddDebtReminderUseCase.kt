package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository

class AddDebtReminderUseCase(
    private val repository: DebtReminderRepository,
) {
    suspend operator fun invoke(reminder: DebtReminder): Resource<Boolean> {
        return repository.addReminder(reminder)
    }
}
