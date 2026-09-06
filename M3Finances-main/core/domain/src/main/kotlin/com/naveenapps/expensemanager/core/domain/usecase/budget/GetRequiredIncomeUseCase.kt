package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetGoalType
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.SavingsStrategy
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import java.util.Date
import kotlin.math.max

class GetRequiredIncomeUseCase(
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(periodType: BudgetPeriod = BudgetPeriod.MONTHLY): Flow<Amount> {
        return combine(
            budgetRepository.getBudgets(),
            savingsGoalRepository.getSavingsGoals(),
            getCurrencyUseCase.invoke(),
        ) { budgets, savingsGoals, currency ->
            var totalRequired = 0.0
            
            for (budget in budgets) {
                if (budget.goalType == BudgetGoalType.EXPENSE) {
                    val prorated = budget.amount / daysInPeriod(budget.periodType) * daysInPeriod(periodType)
                    totalRequired += prorated
                }
            }

            for (goal in savingsGoals) {
                if (goal.isAchieved) continue

                val restant = max(0.0, goal.targetAmount - goal.account.amount)
                if (restant <= 0.0) continue

                when (goal.savingsStrategy) {
                    SavingsStrategy.PERCENTAGE_INCOME -> {
                        val transactions = transactionRepository.getTransactionsByAccountId(goal.accountId)
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, -28)
                        val fourWeeksAgo = calendar.timeInMillis
                        var totalContributed = 0.0
                        for (t in transactions) {
                            if (t.createdOn.time >= fourWeeksAgo && t.type == TransactionType.TRANSFER) {
                                if (t.toAccountId == goal.accountId) {
                                    totalContributed += t.amount.amount
                                } else if (t.fromAccountId == goal.accountId) {
                                    totalContributed -= t.amount.amount
                                }
                            }
                        }
                        val rythmeHebdo = max(0.0, totalContributed / 4.0)
                        val rythmeQuotidien = rythmeHebdo / 7.0
                        totalRequired += rythmeQuotidien * daysInPeriod(periodType)
                    }
                    SavingsStrategy.FIXED -> {
                        val tDate = goal.targetDate
                        if (tDate != null) {
                            val now = Date()
                            val diffMillis = tDate.time - now.time
                            val joursRestants = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                            if (joursRestants <= 0) {
                                totalRequired += restant
                            } else {
                                val rythmeQuotidien = restant / joursRestants.toDouble()
                                totalRequired += rythmeQuotidien * daysInPeriod(periodType)
                            }
                        }
                    }
                }
            }

            getFormattedAmountUseCase(totalRequired, currency)
        }.flowOn(appCoroutineDispatchers.computation)
    }

    private fun daysInPeriod(periodType: BudgetPeriod): Double = when (periodType) {
        BudgetPeriod.DAILY -> 1.0
        BudgetPeriod.WEEKLY -> 7.0
        BudgetPeriod.MONTHLY -> 365.25 / 12
        BudgetPeriod.YEARLY -> 365.25
    }
}
