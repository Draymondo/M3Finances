package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.progress
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GetSavingsGoalsUseCase(
    private val repository: SavingsGoalRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val recalculateGoalEstimateUseCase: RecalculateGoalEstimateUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(): Flow<List<SavingsGoalUiModel>> {
        return combine(
            repository.getSavingsGoals(),
            getCurrencyUseCase.invoke(),
        ) { savingsGoals, currency ->
            savingsGoals.map { savingsGoal ->
                // Call recalculate on the fly for each goal
                val estimateResult = recalculateGoalEstimateUseCase.invoke(savingsGoal)
                
                val estimateMessage = when (estimateResult) {
                    is GoalEstimateResult.Achieved -> "Atteint" // Will be translated in UI if needed
                    is GoalEstimateResult.Indeterminate -> "Date indéterminée"
                    is GoalEstimateResult.Estimated -> {
                        val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                        "Date est. : " + sdf.format(estimateResult.date)
                    }
                }

                SavingsGoalUiModel(
                    savingsGoal = savingsGoal,
                    savedAmount = getFormattedAmountUseCase.invoke(savingsGoal.account.amount, currency),
                    targetAmount = getFormattedAmountUseCase.invoke(savingsGoal.targetAmount, currency),
                    progress = savingsGoal.progress(),
                    estimateMessage = estimateMessage,
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
    }
}

@Stable
data class SavingsGoalUiModel(
    val savingsGoal: SavingsGoal,
    val savedAmount: Amount,
    val targetAmount: Amount,
    val progress: Float,
    val estimateMessage: String? = null,
)
