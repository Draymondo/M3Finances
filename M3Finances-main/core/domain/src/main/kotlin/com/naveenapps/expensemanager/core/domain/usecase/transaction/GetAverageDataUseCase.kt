package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetDateRangeUseCase
import com.naveenapps.expensemanager.core.model.AverageData
import com.naveenapps.expensemanager.core.model.DateRangeModel
import com.naveenapps.expensemanager.core.model.DateRangeType
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.WholeAverageData
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar

class GetAverageDataUseCase(
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    private val getDateRangeUseCase: GetDateRangeUseCase,
    private val dispatcher: AppCoroutineDispatchers,
) {
    fun invoke(): Flow<WholeAverageData> {
        return combine(
            getCurrencyUseCase.invoke(),
            getDateRangeUseCase.invoke(),
            getTransactionWithFilterUseCase.invoke(),
        ) { currency, dateRangeModel, transactions ->
            val incomeAmount =
                transactions?.filter { it.type.isIncome() }?.sumOf { it.amount.amount } ?: 0.0
            val expenseAmount =
                transactions?.filter { it.type.isExpense() }?.sumOf { it.amount.amount } ?: 0.0

            val ranges = dateRangeModel.dateRanges

            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = ranges[0]

            val weeksMultiplier: Double
            val daysMultiplier: Double
            val monthMultiplier: Double

            when (dateRangeModel.type) {
                DateRangeType.TODAY -> {
                    daysMultiplier = 1.0
                    weeksMultiplier = calendar.getActualMaximum(Calendar.DAY_OF_WEEK).toDouble()
                    monthMultiplier = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).toDouble()
                }

                DateRangeType.THIS_WEEK -> {
                    daysMultiplier = 1.0 / calendar.getActualMaximum(Calendar.DAY_OF_WEEK)
                    weeksMultiplier = 1.0
                    monthMultiplier = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH).toDouble()
                }

                DateRangeType.THIS_MONTH -> {
                    daysMultiplier = 1.0 / calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    weeksMultiplier = 1.0 / calendar.getActualMaximum(Calendar.WEEK_OF_MONTH)
                    monthMultiplier = 1.0
                }

                DateRangeType.THIS_YEAR -> {
                    daysMultiplier = 1.0 / calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                    weeksMultiplier = 1.0 / calendar.getActualMaximum(Calendar.WEEK_OF_YEAR)
                    monthMultiplier = 1.0 / 12
                }

                DateRangeType.CUSTOM,
                DateRangeType.ALL,
                    -> {
                    val spanDays = actualSpanDays(dateRangeModel, transactions)
                    daysMultiplier = 1.0 / spanDays
                    weeksMultiplier = 7.0 / spanDays
                    monthMultiplier = (365.25 / 12) / spanDays
                }
            }

            WholeAverageData(
                expenseAverageData = AverageData(
                    perDay = getFormattedAmountUseCase.invoke(
                        (expenseAmount * daysMultiplier),
                        currency,
                    ).amountString.orEmpty(),
                    perWeek = getFormattedAmountUseCase.invoke(
                        (expenseAmount * weeksMultiplier),
                        currency,
                    ).amountString.orEmpty(),
                    perMonth = getFormattedAmountUseCase.invoke(
                        (expenseAmount * monthMultiplier),
                        currency,
                    ).amountString.orEmpty(),
                ),
                incomeAverageData = AverageData(
                    perDay = getFormattedAmountUseCase.invoke(
                        (incomeAmount * daysMultiplier),
                        currency,
                    ).amountString.orEmpty(),
                    perWeek = getFormattedAmountUseCase.invoke(
                        (incomeAmount * weeksMultiplier),
                        currency,
                    ).amountString.orEmpty(),
                    perMonth = getFormattedAmountUseCase.invoke(
                        (incomeAmount * monthMultiplier),
                        currency,
                    ).amountString.orEmpty(),
                ),
            )
        }.flowOn(dispatcher.computation)
    }

    private fun actualSpanDays(
        dateRangeModel: DateRangeModel,
        transactions: List<Transaction>?,
    ): Double {
        val spanMillis = if (dateRangeModel.type == DateRangeType.ALL) {
            val times = transactions?.map { it.createdOn.time }
            val earliest = times?.minOrNull()
            val latest = times?.maxOrNull()
            if (earliest == null || latest == null) 0L else latest - earliest
        } else {
            val ranges = dateRangeModel.dateRanges
            val end = ranges.getOrNull(1) ?: ranges[0]
            end - ranges[0]
        }
        return (spanMillis / MILLIS_PER_DAY).coerceAtLeast(1.0)
    }

    companion object {
        private const val MILLIS_PER_DAY = 24.0 * 60 * 60 * 1000
    }
}
