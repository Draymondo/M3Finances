package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.progress
import com.naveenapps.expensemanager.core.repository.InstallmentPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GetInstallmentPlansUseCase(
    private val repository: InstallmentPlanRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(): Flow<List<InstallmentPlanUiModel>> {
        return combine(
            repository.getInstallmentPlans(),
            getCurrencyUseCase.invoke(),
        ) { plans, currency ->
            plans.map { plan ->
                val paid = plan.account.amount
                InstallmentPlanUiModel(
                    plan = plan,
                    paidAmount = getFormattedAmountUseCase.invoke(paid, currency),
                    targetAmount = getFormattedAmountUseCase.invoke(plan.targetAmount, currency),
                    remainingAmount = getFormattedAmountUseCase.invoke(
                        (plan.targetAmount - paid).coerceAtLeast(0.0),
                        currency,
                    ),
                    progress = plan.progress(),
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
    }
}

@Stable
data class InstallmentPlanUiModel(
    val plan: InstallmentPlan,
    val paidAmount: Amount,
    val targetAmount: Amount,
    val remainingAmount: Amount,
    val progress: Float,
)
