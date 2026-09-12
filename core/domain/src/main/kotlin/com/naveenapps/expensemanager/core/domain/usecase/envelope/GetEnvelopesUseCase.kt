package com.naveenapps.expensemanager.core.domain.usecase.envelope

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.repository.CategoryRepository
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetEnvelopesUseCase(
    private val envelopeRepository: EnvelopeRepository,
    private val categoryRepository: CategoryRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getEnvelopeRemainingUseCase: GetEnvelopeRemainingUseCase,
) {
    operator fun invoke(): Flow<List<EnvelopeUiModel>> {
        return combine(
            getCurrencyUseCase.invoke(),
            categoryRepository.getCategories(),
            envelopeRepository.getEnvelopes(),
        ) { currency, categories, envelopes ->
            envelopes.map { envelope ->
                val category = categories.find { it.id == envelope.categoryId }
                val remaining = when (
                    val result = getEnvelopeRemainingUseCase(
                        envelopeAmount = envelope.amount,
                        categoryId = envelope.categoryId,
                        selectedMonth = envelope.selectedMonth,
                        periodType = envelope.periodType,
                    )
                ) {
                    is Resource.Error -> EnvelopeRemaining(0.0, envelope.amount, 0f, false)
                    is Resource.Success -> result.data
                }
                EnvelopeUiModel(
                    id = envelope.id,
                    categoryId = envelope.categoryId,
                    categoryName = category?.name ?: envelope.categoryId,
                    categoryIcon = category?.storedIcon,
                    name = envelope.name,
                    selectedMonth = envelope.selectedMonth,
                    periodType = envelope.periodType,
                    amount = getFormattedAmountUseCase(envelope.amount, currency),
                    spentAmount = getFormattedAmountUseCase(remaining.spent, currency),
                    remainingAmount = getFormattedAmountUseCase(remaining.remaining, currency),
                    percent = remaining.percent,
                    isExceeded = remaining.isExceeded,
                )
            }
        }
    }
}

@Stable
data class EnvelopeUiModel(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: StoredIcon?,
    val name: String?,
    val selectedMonth: String,
    val periodType: BudgetPeriod,
    val amount: Amount,
    val spentAmount: Amount,
    val remainingAmount: Amount,
    val percent: Float,
    val isExceeded: Boolean,
)
