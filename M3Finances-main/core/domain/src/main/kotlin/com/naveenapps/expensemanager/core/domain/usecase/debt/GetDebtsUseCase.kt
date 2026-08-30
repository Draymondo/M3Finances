package com.naveenapps.expensemanager.core.domain.usecase.debt

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlin.math.abs

class GetDebtsUseCase(
    private val repository: DebtRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(): Flow<List<DebtUiModel>> {
        return combine(
            repository.getDebts(),
            getCurrencyUseCase.invoke(),
        ) { debts, currency ->
            debts.map { debt ->
                DebtUiModel(
                    debt = debt,
                    // The debt account's balance carries the sign (negative when the user
                    // borrowed); the direction, not the sign, is what the UI shows to the person.
                    remainingAmount = getFormattedAmountUseCase.invoke(
                        abs(debt.account.amount),
                        currency,
                    ),
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
    }
}

@Stable
data class DebtUiModel(
    val debt: Debt,
    val remainingAmount: Amount,
)
