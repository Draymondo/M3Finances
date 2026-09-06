package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository

/** Updates a goal's metadata (name, target amount, target date, notes, or the manual
 * [SavingsGoal.isAchieved] flag). Doesn't touch the hidden account or any transaction — use
 * [AddSavingsGoalContributionUseCase] for that. */
class UpdateSavingsGoalUseCase(
    private val repository: SavingsGoalRepository,
) {
    suspend operator fun invoke(savingsGoal: SavingsGoal): Resource<Boolean> {
        if (savingsGoal.name.isBlank()) {
            return Resource.Error(Exception("Name shouldn't be blank"))
        }

        if (savingsGoal.targetAmount <= 0.0) {
            return Resource.Error(Exception("Target amount should be greater than 0"))
        }

        if (savingsGoal.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME) {
            val pct = savingsGoal.targetPercentage
            if (pct == null || pct <= 0.0 || pct > 100.0) {
                return Resource.Error(Exception("Target percentage must be between 0 and 100"))
            }
        }

        return repository.updateSavingsGoal(savingsGoal)
    }
}
