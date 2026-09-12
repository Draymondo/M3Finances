package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource

/**
 * How much of an envelope's cap is still available for a category + period, right now — used
 * both by the envelope list/detail screens and, in a later phase, by the transaction entry
 * screen to warn in real time when the amount being typed would exceed (or already has
 * exceeded) the envelope. Independent of any [com.naveenapps.expensemanager.core.model.Envelope]
 * instance so the transaction screen can call it with just the category/period the user picked,
 * without first loading the envelope object.
 */
class GetEnvelopeRemainingUseCase(
    private val getEnvelopeTransactionsUseCase: GetEnvelopeTransactionsUseCase,
) {
    suspend operator fun invoke(
        envelopeAmount: Double,
        categoryId: String,
        selectedMonth: String,
        periodType: BudgetPeriod,
    ): Resource<EnvelopeRemaining> {
        return when (
            val transactions = getEnvelopeTransactionsUseCase(
                categoryId = categoryId,
                selectedMonth = selectedMonth,
                periodType = periodType,
            )
        ) {
            is Resource.Error -> transactions
            is Resource.Success -> {
                val spent = transactions.data.sumOf { it.amount.amount }
                val remaining = envelopeAmount - spent
                val percent = if (envelopeAmount > 0.0) {
                    (spent / envelopeAmount).toFloat() * 100
                } else {
                    0f
                }
                Resource.Success(
                    EnvelopeRemaining(
                        spent = spent,
                        remaining = remaining,
                        percent = percent,
                        isExceeded = remaining < 0.0,
                    ),
                )
            }
        }
    }
}

data class EnvelopeRemaining(
    val spent: Double,
    val remaining: Double,
    val percent: Float,
    val isExceeded: Boolean,
)
