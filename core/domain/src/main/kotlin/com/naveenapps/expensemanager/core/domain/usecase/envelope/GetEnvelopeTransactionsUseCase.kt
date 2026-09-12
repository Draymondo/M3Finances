package com.naveenapps.expensemanager.core.domain.usecase.envelope

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
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.expandedForCategoryAccounting
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Expense transactions counting toward a single category for a given period key — same
 * period-window logic as `GetBudgetTransactionsUseCase`, but for one category (an envelope has
 * no account filter: it applies regardless of which account the spending came from — so, like
 * a budget with `isAllAccountsSelected`, every account id is passed explicitly, since the
 * underlying SQL `IN(:accounts)` clause matches nothing on an empty list) and restricted to
 * [TransactionType.EXPENSE] (an envelope is always a spending cap).
 */
class GetEnvelopeTransactionsUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        categoryId: String,
        selectedMonth: String,
        periodType: BudgetPeriod,
    ): Resource<List<Transaction>> {
        val accounts = accountRepository.getAccounts().firstOrNull()?.map { it.id } ?: emptyList()

        val date = when (periodType) {
            BudgetPeriod.YEARLY -> selectedMonth.fromYear()
            BudgetPeriod.MONTHLY -> selectedMonth.fromMonthAndYearKey()
            BudgetPeriod.WEEKLY -> selectedMonth.fromWeekKey()
            BudgetPeriod.DAILY -> selectedMonth.fromDayKey()
        }

        date ?: return Resource.Error(IllegalArgumentException("Valeur de date inconnue"))

        val startDate = when (periodType) {
            BudgetPeriod.YEARLY -> date.getStartOfTheYear()
            BudgetPeriod.MONTHLY -> date.getStartOfTheMonth()
            BudgetPeriod.WEEKLY -> date.getStartOfTheWeek()
            BudgetPeriod.DAILY -> date.getStartOfTheDay()
        }
        val endDate = when (periodType) {
            BudgetPeriod.YEARLY -> date.getEndOfTheYear()
            BudgetPeriod.MONTHLY -> date.getEndOfTheMonth()
            BudgetPeriod.WEEKLY -> date.getEndOfTheWeek()
            BudgetPeriod.DAILY -> date.getEndOfTheDay()
        }

        val transactions = transactionRepository.getFilteredTransaction(
            accounts = accounts,
            categories = listOf(categoryId),
            transactionType = listOf(TransactionType.EXPENSE.ordinal),
            startDate = startDate,
            endDate = endDate,
        ).firstOrNull()?.filter {
            when (periodType) {
                BudgetPeriod.YEARLY -> it.createdOn.toYear() == selectedMonth
                BudgetPeriod.MONTHLY -> it.createdOn.toMonthAndYearKey() == selectedMonth
                BudgetPeriod.WEEKLY -> it.createdOn.toWeekKey() == selectedMonth
                BudgetPeriod.DAILY -> it.createdOn.toDayKey() == selectedMonth
            }
        }?.filter { it.type.isExpense() }

        return Resource.Success(transactions?.expandedForCategoryAccounting() ?: emptyList())
    }
}
