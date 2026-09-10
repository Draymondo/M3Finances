package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class GetBudgetDetailUseCase(
    private val budgetRepository: BudgetRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase,
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    private val getBudgetEquivalentsUseCase: GetBudgetEquivalentsUseCase,
    private val calculateBudgetRolloverUseCase: CalculateBudgetRolloverUseCase,
) {
    operator fun invoke(budgetId: String): Flow<BudgetUiModel?> {
        return combine(
            budgetRepository.findBudgetByIdFlow(budgetId),
            getTransactionWithFilterUseCase.invoke(),
        ) { budget, _ ->
            budget?.let {
                val currency = getCurrencyUseCase.invoke().first()
                val budgetTransactions =
                    when (val transaction = getBudgetTransactionsUseCase.invoke(budget)) {
                        is Resource.Error -> {
                            null
                        }

                        is Resource.Success -> {
                            transaction.data.filter {
                                if (budget.goalType == com.naveenapps.expensemanager.core.model.BudgetGoalType.INCOME) {
                                    it.type.isIncome()
                                } else {
                                    it.type.isExpense()
                                }
                            }
                        }
                    }
                val transactionAmount = budgetTransactions?.sumOf { it.amount.amount } ?: 0.0
                val rollover = calculateBudgetRolloverUseCase.invoke(budget)
                val effectiveLimit = budget.amount + rollover
                val percent = if (effectiveLimit > 0.0) {
                    (transactionAmount / effectiveLimit).toFloat() * 100
                } else {
                    0f
                }
                val equivalents = getBudgetEquivalentsUseCase.invoke(
                    amount = effectiveLimit,
                    periodType = budget.periodType,
                    accounts = budget.accounts,
                    categories = budget.categories,
                    isAllAccountsSelected = budget.isAllAccountsSelected,
                    isAllCategoriesSelected = budget.isAllCategoriesSelected,
                    goalType = budget.goalType,
                )
                budget.toBudgetUiModel(
                    budgetAmount = getFormattedAmountUseCase(effectiveLimit, currency),
                    transactionAmount = getFormattedAmountUseCase(transactionAmount, currency),
                    percent = percent,
                    budgetTransactions?.map {
                        it.toTransactionUIModel(
                            getFormattedAmountUseCase(it.amount.amount, currency),
                        )
                    },
                    equivalents,
                )
            }
        }
    }
}
