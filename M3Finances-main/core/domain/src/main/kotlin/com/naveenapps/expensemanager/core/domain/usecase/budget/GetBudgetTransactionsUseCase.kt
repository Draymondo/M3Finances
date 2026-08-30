package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.common.utils.fromDayKey
import com.naveenapps.expensemanager.core.common.utils.fromMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.fromWeekKey
import com.naveenapps.expensemanager.core.common.utils.fromYear
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheYear
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheYear
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.expandedForCategoryAccounting
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.CategoryRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class GetBudgetTransactionsUseCase(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(budget: Budget): Resource<List<Transaction>> {
        val accounts: List<String> = if (budget.isAllAccountsSelected) {
            accountRepository.getAccounts().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            budget.accounts
        }

        val categories: List<String> = if (budget.isAllCategoriesSelected) {
            categoryRepository.getCategories().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            budget.categories
        }

        val date = when (budget.periodType) {
            BudgetPeriod.YEARLY -> budget.selectedMonth.fromYear()
            BudgetPeriod.MONTHLY -> budget.selectedMonth.fromMonthAndYearKey()
            BudgetPeriod.WEEKLY -> budget.selectedMonth.fromWeekKey()
            BudgetPeriod.DAILY -> budget.selectedMonth.fromDayKey()
        }

        date ?: return Resource.Error(IllegalArgumentException("Unknown month value"))

        val startDate = when (budget.periodType) {
            BudgetPeriod.YEARLY -> date.getStartOfTheYear()
            BudgetPeriod.MONTHLY -> date.getStartOfTheMonth()
            BudgetPeriod.WEEKLY -> date.getStartOfTheWeek()
            BudgetPeriod.DAILY -> date.getStartOfTheDay()
        }
        val endDate = when (budget.periodType) {
            BudgetPeriod.YEARLY -> date.getEndOfTheYear()
            BudgetPeriod.MONTHLY -> date.getEndOfTheMonth()
            BudgetPeriod.WEEKLY -> date.getEndOfTheWeek()
            BudgetPeriod.DAILY -> date.getEndOfTheDay()
        }

        val transaction = transactionRepository.getFilteredTransaction(
            accounts = accounts,
            categories = categories,
            transactionType = TransactionType.entries.map { it.ordinal }.toList(),
            startDate = startDate,
            endDate = endDate,
        ).firstOrNull()?.filter {
            when (budget.periodType) {
                BudgetPeriod.YEARLY -> it.createdOn.toYear() == budget.selectedMonth
                BudgetPeriod.MONTHLY -> it.createdOn.toMonthAndYearKey() == budget.selectedMonth
                BudgetPeriod.WEEKLY -> it.createdOn.toWeekKey() == budget.selectedMonth
                BudgetPeriod.DAILY -> it.createdOn.toDayKey() == budget.selectedMonth
            }
        }

        return Resource.Success(transaction?.expandedForCategoryAccounting() ?: emptyList())
    }
}
