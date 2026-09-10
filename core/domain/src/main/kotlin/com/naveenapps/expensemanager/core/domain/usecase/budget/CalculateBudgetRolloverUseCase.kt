package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.common.utils.fromDayKey
import com.naveenapps.expensemanager.core.common.utils.fromMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.fromWeekKey
import com.naveenapps.expensemanager.core.common.utils.fromYear
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetGoalType
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Date

class CalculateBudgetRolloverUseCase(
    private val budgetRepository: BudgetRepository,
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase,
) {
    suspend operator fun invoke(budget: Budget, depth: Int = 0): Double {
        if (depth > 12) return 0.0 // Safety limit to avoid infinite recursion

        val previousPeriodKey = getPreviousPeriodKey(budget.selectedMonth, budget.periodType) ?: return 0.0
        val previousBudget = findMatchingBudget(budget, previousPeriodKey) ?: return 0.0

        val transactions = when (val response = getBudgetTransactionsUseCase(previousBudget)) {
            is Resource.Error -> null
            is Resource.Success -> response.data.filter {
                if (previousBudget.goalType == BudgetGoalType.INCOME) {
                    it.type.isIncome()
                } else {
                    it.type.isExpense()
                }
            }
        }
        val spentInPrevious = transactions?.sumOf { it.amount.amount } ?: 0.0
        val previousRollover = invoke(previousBudget, depth + 1)

        val unspent = previousBudget.amount + previousRollover - spentInPrevious
        // Rollover only positive savings to encourage saving and keep UI clean
        return if (unspent > 0.0) unspent else 0.0
    }

    private fun getPreviousPeriodKey(selectedMonth: String, periodType: BudgetPeriod): String? {
        val date = when (periodType) {
            BudgetPeriod.YEARLY -> selectedMonth.fromYear()
            BudgetPeriod.MONTHLY -> selectedMonth.fromMonthAndYearKey()
            BudgetPeriod.WEEKLY -> selectedMonth.fromWeekKey()
            BudgetPeriod.DAILY -> selectedMonth.fromDayKey()
        } ?: return null

        val calendar = Calendar.getInstance()
        calendar.time = date

        when (periodType) {
            BudgetPeriod.YEARLY -> calendar.add(Calendar.YEAR, -1)
            BudgetPeriod.MONTHLY -> calendar.add(Calendar.MONTH, -1)
            BudgetPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            BudgetPeriod.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val previousDate = calendar.time
        return when (periodType) {
            BudgetPeriod.YEARLY -> previousDate.toYear()
            BudgetPeriod.MONTHLY -> previousDate.toMonthAndYearKey()
            BudgetPeriod.WEEKLY -> previousDate.toWeekKey()
            BudgetPeriod.DAILY -> previousDate.toDayKey()
        }
    }

    private suspend fun findMatchingBudget(budget: Budget, previousPeriodKey: String): Budget? {
        val allBudgets = budgetRepository.getBudgets().firstOrNull() ?: return null
        return allBudgets.find { b ->
            b.periodType == budget.periodType &&
                    b.selectedMonth == previousPeriodKey &&
                    b.isAllAccountsSelected == budget.isAllAccountsSelected &&
                    b.isAllCategoriesSelected == budget.isAllCategoriesSelected &&
                    b.accounts.sorted() == budget.accounts.sorted() &&
                    b.categories.sorted() == budget.categories.sorted()
        }
    }
}

