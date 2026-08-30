package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository

class FindSavingsGoalByIdUseCase(
    private val repository: SavingsGoalRepository,
) {
    suspend operator fun invoke(id: String): Resource<SavingsGoal> {
        return repository.findSavingsGoalById(id)
    }
}
