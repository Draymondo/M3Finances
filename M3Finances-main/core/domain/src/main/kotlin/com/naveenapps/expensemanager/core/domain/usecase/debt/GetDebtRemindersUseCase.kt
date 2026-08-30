package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository
import kotlinx.coroutines.flow.Flow

class GetDebtRemindersUseCase(
    private val repository: DebtReminderRepository,
) {
    operator fun invoke(debtId: String): Flow<List<DebtReminder>> {
        return repository.getRemindersForDebt(debtId)
    }
}
