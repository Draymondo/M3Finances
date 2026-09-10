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
            return Resource.Error(Exception("Le nom ne doit pas être vide"))
        }

        if (savingsGoal.targetAmount <= 0.0) {
            return Resource.Error(Exception("Le montant cible doit être supérieur à 0"))
        }

        if (savingsGoal.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME) {
            val pct = savingsGoal.targetPercentage
            if (pct == null || pct <= 0.0 || pct > 100.0) {
                return Resource.Error(Exception("Le pourcentage cible doit être compris entre 0 et 100"))
            }
        }

        return repository.updateSavingsGoal(savingsGoal)
    }
}
