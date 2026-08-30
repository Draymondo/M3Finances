package com.naveenapps.expensemanager.core.domain.usecase.budget

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheYear
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheYear
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.expandedForCategoryAccounting
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.CategoryRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

/**
 * Given an amount entered for one [BudgetPeriod], shows what that pace works out to at the other
 * three granularities *right now* — e.g. entering 5000 for a MONTHLY budget also shows what
 * ~1152/week and ~164/day (and 60000/year) look like, each compared against what's actually been
 * spent so far in *today's* current day/week/month/year. Deliberately anchored to the current
 * period rather than to whatever specific period instance the source budget covers, so a budget
 * for a past year still shows a meaningful "at today's pace" comparison — see the design
 * discussion this was built from.
 *
 * Purely a read-only, on-the-fly comparison: it never creates, updates, or deletes any budget.
 * Call this any time (create screen while typing, or the detail screen) to get a fresh number;
 * nothing here is persisted, so there's no linked budget to keep in sync or clean up.
 */
class GetBudgetEquivalentsUseCase(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
) {
    suspend operator fun invoke(
        amount: Double,
        periodType: BudgetPeriod,
        accounts: List<String>,
        categories: List<String>,
        isAllAccountsSelected: Boolean,
        isAllCategoriesSelected: Boolean,
    ): List<BudgetEquivalentUiModel> {
        if (amount <= 0.0) return emptyList()

        val resolvedAccounts = if (isAllAccountsSelected) {
            accountRepository.getAccounts().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            accounts
        }

        val resolvedCategories = if (isAllCategoriesSelected) {
            categoryRepository.getCategories().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            categories
        }

        val currency = getCurrencyUseCase.invoke().first()
        val now = Date()

        return PERIOD_DISPLAY_ORDER
            .filter { it != periodType }
            .map { targetPeriod ->
                val equivalentAmount =
                    amount / daysInPeriod(periodType) * daysInPeriod(targetPeriod)

                val startDate = when (targetPeriod) {
                    BudgetPeriod.YEARLY -> now.getStartOfTheYear()
                    BudgetPeriod.MONTHLY -> now.getStartOfTheMonth()
                    BudgetPeriod.WEEKLY -> now.getStartOfTheWeek()
                    BudgetPeriod.DAILY -> now.getStartOfTheDay()
                }
                val endDate = when (targetPeriod) {
                    BudgetPeriod.YEARLY -> now.getEndOfTheYear()
                    BudgetPeriod.MONTHLY -> now.getEndOfTheMonth()
                    BudgetPeriod.WEEKLY -> now.getEndOfTheWeek()
                    BudgetPeriod.DAILY -> now.getEndOfTheDay()
                }

                val spent = transactionRepository.getFilteredTransaction(
                    accounts = resolvedAccounts,
                    categories = resolvedCategories,
                    transactionType = TransactionType.entries.map { it.ordinal },
                    startDate = startDate,
                    endDate = endDate,
                ).firstOrNull()
                    ?.expandedForCategoryAccounting()
                    ?.filter { it.type.isExpense() }
                    ?.sumOf { it.amount.amount }
                    ?: 0.0

                val percent = if (equivalentAmount > 0.0) {
                    (spent / equivalentAmount).toFloat() * 100
                } else {
                    0f
                }

                BudgetEquivalentUiModel(
                    periodType = targetPeriod,
                    equivalentAmount = getFormattedAmountUseCase(equivalentAmount, currency),
                    spentAmount = getFormattedAmountUseCase(spent, currency),
                    percent = percent,
                    progressBarColor = budgetProgressColor(percent),
                )
            }
    }

    companion object {
        /** Average day counts, not calendar-exact (a month isn't always 30.4375 days) — picked
         * so a conversion gives the same numbers regardless of which period you start from. */
        private fun daysInPeriod(periodType: BudgetPeriod): Double = when (periodType) {
            BudgetPeriod.DAILY -> 1.0
            BudgetPeriod.WEEKLY -> 7.0
            BudgetPeriod.MONTHLY -> 365.25 / 12
            BudgetPeriod.YEARLY -> 365.25
        }

        private val PERIOD_DISPLAY_ORDER = listOf(
            BudgetPeriod.YEARLY,
            BudgetPeriod.MONTHLY,
            BudgetPeriod.WEEKLY,
            BudgetPeriod.DAILY,
        )
    }
}

@Stable
data class BudgetEquivalentUiModel(
    val periodType: BudgetPeriod,
    val equivalentAmount: Amount,
    val spentAmount: Amount,
    val percent: Float,
    val progressBarColor: Int,
)
