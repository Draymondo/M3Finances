package com.naveenapps.expensemanager.core.domain.usecase.budget

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.R
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.fromDayKey
import com.naveenapps.expensemanager.core.common.utils.fromMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.fromWeekKey
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetBudgetsUseCase(
    private val budgetRepository: BudgetRepository,
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase,
    private val calculateBudgetRolloverUseCase: CalculateBudgetRolloverUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers
) {
    operator fun invoke(): Flow<List<BudgetUiModel>> {
        return combine(
            getCurrencyUseCase.invoke(),
            getTransactionWithFilterUseCase.invoke(),
            budgetRepository.getBudgets(),
        ) { currency, _, budgets ->
            budgets.map { budget ->
                val transactions = when (val response = getBudgetTransactionsUseCase.invoke(budget)) {
                    is Resource.Error -> null
                    is Resource.Success -> response.data.filter { 
                        if (budget.goalType == com.naveenapps.expensemanager.core.model.BudgetGoalType.INCOME) {
                            it.type.isIncome()
                        } else {
                            it.type.isExpense()
                        }
                    }
                }
                val transactionAmount = transactions?.sumOf { it.amount.amount } ?: 0.0
                val rollover = calculateBudgetRolloverUseCase.invoke(budget)
                val effectiveLimit = budget.amount + rollover
                val percent = if (effectiveLimit > 0.0) {
                    (transactionAmount / effectiveLimit).toFloat() * 100
                } else {
                    0f
                }
                budget.toBudgetUiModel(
                    name = budgetName(budget.selectedMonth, budget.periodType),
                    budgetAmount = getFormattedAmountUseCase(effectiveLimit, currency),
                    transactionAmount = getFormattedAmountUseCase(transactionAmount, currency),
                    percent,
                    transactions?.map {
                        it.toTransactionUIModel(getFormattedAmountUseCase(it.amount.amount, currency))
                    },
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
    }
}

private val shortMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
private val shortDayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

fun budgetName(selectedMonth: String, periodType: BudgetPeriod = BudgetPeriod.MONTHLY): String {
    return when (periodType) {
        BudgetPeriod.YEARLY -> {
            val currentYear = Date().toYear()
            if (selectedMonth == currentYear) {
                "This Year Budget"
            } else {
                "$selectedMonth Budget"
            }
        }

        BudgetPeriod.MONTHLY -> {
            val currentMonth = Date().toMonthAndYearKey()
            if (selectedMonth == currentMonth) {
                "This Month Budget"
            } else {
                val short = selectedMonth.fromMonthAndYearKey()
                    ?.let { shortMonthFormat.format(it) }
                    ?: selectedMonth
                "$short Budget"
            }
        }

        BudgetPeriod.WEEKLY -> {
            val currentWeek = Date().toWeekKey()
            if (selectedMonth == currentWeek) {
                "This Week Budget"
            } else {
                val short = selectedMonth.fromWeekKey()
                    ?.let { shortDayFormat.format(it) }
                    ?: selectedMonth
                "Week of $short Budget"
            }
        }

        BudgetPeriod.DAILY -> {
            val currentDay = Date().toDayKey()
            if (selectedMonth == currentDay) {
                "Today Budget"
            } else {
                val short = selectedMonth.fromDayKey()
                    ?.let { shortDayFormat.format(it) }
                    ?: selectedMonth
                "$short Budget"
            }
        }
    }
}

/** Shared with `GetBudgetEquivalentsUseCase.progressBarColor` so a budget and its equivalence
 * breakdown always use the same color thresholds for the same percentage. */
fun budgetProgressColor(percent: Float, goalType: com.naveenapps.expensemanager.core.model.BudgetGoalType = com.naveenapps.expensemanager.core.model.BudgetGoalType.EXPENSE): Int {
    if (goalType == com.naveenapps.expensemanager.core.model.BudgetGoalType.INCOME) {
        return when {
            percent < 0f -> R.color.red_500
            percent in 0f..35f -> R.color.red_500
            percent in 36f..60f -> R.color.orange_500
            percent in 61f..85f -> R.color.light_green_500
            else -> R.color.green_500
        }
    }
    return when {
        percent < 0f -> R.color.green_500
        percent in 0f..35f -> R.color.green_500
        percent in 36f..60f -> R.color.light_green_500
        percent in 61f..85f -> R.color.orange_500
        else -> R.color.red_500
    }
}

fun Budget.toBudgetUiModel(
    name: String,
    budgetAmount: Amount,
    transactionAmount: Amount,
    percent: Float,
    transactions: List<TransactionUiItem>? = null,
    equivalents: List<BudgetEquivalentUiModel>? = null,
) = BudgetUiModel(
    id = this.id,
    name = name,
    selectedMonth = this.selectedMonth,
    periodType = this.periodType,
    goalType = this.goalType,
    progressBarColor = budgetProgressColor(percent, this.goalType),
    amount = budgetAmount,
    transactionAmount = transactionAmount,
    percent = percent,
    transactions = transactions,
    equivalents = equivalents,
)

@Stable
data class BudgetUiModel(
    val id: String,
    val name: String,
    val selectedMonth: String,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val goalType: com.naveenapps.expensemanager.core.model.BudgetGoalType = com.naveenapps.expensemanager.core.model.BudgetGoalType.EXPENSE,
    val progressBarColor: Int,
    val amount: Amount,
    val transactionAmount: Amount,
    val percent: Float,
    val transactions: List<TransactionUiItem>? = null,
    /** Only populated by `GetBudgetDetailUseCase` — left null in the list (`GetBudgetsUseCase`)
     * to avoid tripling the transaction queries for every row just to show a list. See
     * `GetBudgetEquivalentsUseCase`. */
    val equivalents: List<BudgetEquivalentUiModel>? = null,
)
