package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.SavingsStrategy
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.first

class SuggestContributionUseCase(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    /**
     * Pour une transaction de type revenu donnée, calcule la contribution suggérée pour chaque
     * objectif d'épargne actif configuré en pourcentage du revenu.
     */
    suspend operator fun invoke(incomeAmount: Double): Map<SavingsGoal, Double> {
        if (incomeAmount <= 0.0) return emptyMap()

        val goals = savingsGoalRepository.getSavingsGoals().first()
        val suggestions = mutableMapOf<SavingsGoal, Double>()

        for (goal in goals) {
            if (goal.savingsStrategy == SavingsStrategy.PERCENTAGE_INCOME && !goal.isAchieved) {
                val percentage = goal.targetPercentage ?: 0.0
                if (percentage > 0.0) {
                    val contribution = incomeAmount * (percentage / 100.0)
                    suggestions[goal] = contribution
                }
            }
        }

        return suggestions
    }
}

