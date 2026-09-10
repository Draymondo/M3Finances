package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import java.util.Calendar
import java.util.Date
import kotlin.math.max

sealed class GoalEstimateResult {
    object Achieved : GoalEstimateResult()
    object Indeterminate : GoalEstimateResult()
    data class Estimated(val date: Date) : GoalEstimateResult()
}

class RecalculateGoalEstimateUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(savingsGoal: SavingsGoal): GoalEstimateResult {
        if (savingsGoal.isAchieved) return GoalEstimateResult.Achieved // Ne pas calculer pour les objectifs fermés manuellement
        
        val restant = max(0.0, savingsGoal.targetAmount - savingsGoal.account.amount)
        if (restant == 0.0) return GoalEstimateResult.Achieved // Déjà atteint mathématiquement

        // Get transactions for the goal's hidden account
        val transactions = transactionRepository.getTransactionsByAccountId(savingsGoal.accountId)

        // Filter transactions from the last 28 days (4 weeks)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -28)
        val fourWeeksAgo = calendar.timeInMillis

        var totalContributed = 0.0

        for (transaction in transactions) {
            if (transaction.createdOn.time >= fourWeeksAgo && transaction.type == TransactionType.TRANSFER) {
                // If it's a contribution to the goal
                if (transaction.toAccountId == savingsGoal.accountId) {
                    totalContributed += transaction.amount.amount
                }
                // If it's a withdrawal from the goal
                else if (transaction.fromAccountId == savingsGoal.accountId) {
                    totalContributed -= transaction.amount.amount
                }
            }
        }

        val rythmeHebdo = totalContributed / 4.0

        if (rythmeHebdo <= 0.0) {
            return GoalEstimateResult.Indeterminate // Date indéterminée
        }

        val semainesRestantes = restant / rythmeHebdo
        val daysRemaining = (semainesRestantes * 7).toInt()

        val futureCalendar = Calendar.getInstance()
        futureCalendar.add(Calendar.DAY_OF_YEAR, daysRemaining)
        return GoalEstimateResult.Estimated(futureCalendar.time)
    }
}

