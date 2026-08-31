package com.naveenapps.expensemanager.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.budgetName
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetDateRangeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionGroupByCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.CategoryTransactionState
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.DateRangeType
import com.naveenapps.expensemanager.core.model.ExpenseFlowState
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.Date


class DashboardViewModel(
    getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    getCurrencyUseCase: GetCurrencyUseCase,
    getFormattedAmountUseCase: GetFormattedAmountUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getTransactionGroupByCategoryUseCase: GetTransactionGroupByCategoryUseCase,
    getBudgetsUseCase: GetBudgetsUseCase,
    appCoroutineDispatchers: AppCoroutineDispatchers,
    getDateRangeUseCase: GetDateRangeUseCase,
    settingsRepository: SettingsRepository,
    getPendingTransactionsUseCase: com.naveenapps.expensemanager.core.domain.usecase.transaction.GetPendingTransactionsUseCase,
    private val appComposeNavigator: AppComposeNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(
        DashboardState(
            expenseFlowState = ExpenseFlowState(),
            transactions = emptyList(),
            budgets = emptyList(),
            accounts = emptyList(),
            categoryTransactionState = CategoryTransactionState(
                pieChartData = listOf(),
                totalAmount = Amount(0.0),
                categoryTransactions = emptyList(),
                categoryType = CategoryType.EXPENSE,
            ),
            transactionPeriod = ""
        )
    )
    val state = _state.asStateFlow()

    init {
        combine(
            getCurrencyUseCase.invoke(),
            getTransactionWithFilterUseCase.invoke(),
            getAllAccountsUseCase.invoke(),
            getDateRangeUseCase.invoke(),
        ) { currency, transactions, accounts, dateRange ->

            val filteredTransactions = (transactions?.map {
                it.toTransactionUIModel(
                    getFormattedAmountUseCase.invoke(
                        it.amount.amount,
                        currency,
                    ),
                )
            } ?: emptyList()).take(MAX_TRANSACTIONS_IN_LIST)

            val accountsConverted = accounts.map {
                it.toAccountUiModel(
                    getFormattedAmountUseCase.invoke(
                        it.amount,
                        currency,
                    ),
                    if (it.type == AccountType.CREDIT) {
                        getFormattedAmountUseCase.invoke(
                            it.getAvailableCreditLimit(),
                            currency
                        )
                    } else {
                        null
                    }
                )
            }

            val incomeValue = transactions?.filter { it.type == TransactionType.INCOME }?.sumOf {
                it.amount.amount
            } ?: 0.0

            val expenseValue = transactions?.filter { it.type == TransactionType.EXPENSE }?.sumOf {
                it.amount.amount
            } ?: 0.0


            _state.update {
                it.copy(
                    expenseFlowState = it.expenseFlowState.copy(
                        income = getFormattedAmountUseCase.invoke(
                            incomeValue,
                            currency,
                        ).amountString.orEmpty(),
                        expense = getFormattedAmountUseCase.invoke(
                            expenseValue,
                            currency,
                        ).amountString.orEmpty(),
                        balance = getFormattedAmountUseCase.invoke(
                            (incomeValue - expenseValue),
                            currency,
                        ).amountString.orEmpty(),
                    ),
                    transactions = filteredTransactions,
                    accounts = accountsConverted,
                    transactionPeriod = if (dateRange.description.isNotEmpty()) {
                        "${dateRange.name} (${dateRange.description})"
                    } else {
                        dateRange.name
                    }
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
            .launchIn(viewModelScope)

        getTransactionGroupByCategoryUseCase.invoke(CategoryType.EXPENSE).onEach {
            val categoryTransaction = it.copy(
                pieChartData = it.pieChartData.take(4),
                categoryTransactions = it.categoryTransactions.take(4),
            )
            _state.update { state -> state.copy(categoryTransactionState = categoryTransaction) }
        }.launchIn(viewModelScope)

        getPendingTransactionsUseCase.invoke().onEach { pendingList ->
            _state.update { it.copy(pendingTransactionsCount = pendingList.size) }
        }.launchIn(viewModelScope)

        combine(
            getBudgetsUseCase.invoke(),
            getDateRangeUseCase.invoke(),
        ) { allBudgets, dateRange ->
            val activeDate = when (dateRange.type) {
                DateRangeType.TODAY, DateRangeType.THIS_WEEK, DateRangeType.THIS_MONTH ->
                    Date(dateRange.dateRanges[0])
                else -> null
            }
            val activeMonth = activeDate?.toMonthAndYearKey()
            val activeWeek = activeDate?.toWeekKey()
            val activeDay = activeDate?.toDayKey()
            val filtered = if (activeMonth != null) {
                // "Active Budgets" means: the monthly budget covering the month being viewed,
                // together with the yearly budget covering the current year, the weekly budget
                // covering the week being viewed, and the daily budget covering the day being
                // viewed — all can be active at once (e.g. a monthly grocery budget alongside a
                // yearly travel budget).
                val currentYear = Date().toYear()
                allBudgets.filter { budget ->
                    when (budget.periodType) {
                        BudgetPeriod.MONTHLY -> budget.selectedMonth == activeMonth
                        BudgetPeriod.YEARLY -> budget.selectedMonth == currentYear
                        BudgetPeriod.WEEKLY -> budget.selectedMonth == activeWeek
                        BudgetPeriod.DAILY -> budget.selectedMonth == activeDay
                    }
                }
            } else {
                allBudgets
            }
            // The "create a budget for this month" nudge is about monthly budgets specifically —
            // an active yearly budget shouldn't suppress it.
            val hasMonthlyBudget = filtered.any { it.periodType == BudgetPeriod.MONTHLY }
            val showCreateBudgetForMonth = if (activeMonth != null && !hasMonthlyBudget) {
                budgetName(activeMonth)
            } else {
                null
            }
            _state.update { it.copy(budgets = filtered, showCreateBudgetForMonth = showCreateBudgetForMonth) }
        }.flowOn(appCoroutineDispatchers.computation)
            .launchIn(viewModelScope)

        settingsRepository.getHomeSummaryCompact().onEach { compact ->
            _state.update { it.copy(isCompactSummary = compact) }
        }.launchIn(viewModelScope)
    }

    private fun openSettings() {
        appComposeNavigator.navigate(ExpenseManagerScreens.Settings)
    }

    private fun openAccountList() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountList)
    }

    private fun openAccountCreate(accountId: String?) {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountCreate(accountId))
    }

    private fun openBudgetList() {
        appComposeNavigator.navigate(ExpenseManagerScreens.BudgetList)
    }

    private fun openBudgetCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.BudgetCreate(null))
    }

    private fun openBudgetDetails(budgetId: String?) {
        appComposeNavigator.navigate(ExpenseManagerScreens.BudgetDetails(budgetId))
    }

    private fun openTransactionList() {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionList)
    }

    private fun openTransactionCreate(transactionId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(transactionId))
    }

    fun processAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.OpenAccountEdit -> openAccountCreate(action.account.id)
            DashboardAction.OpenAccountList -> openAccountList()
            is DashboardAction.OpenBudgetDetails -> openBudgetDetails(action.budgetUiModel.id)
            DashboardAction.OpenBudgetList -> openBudgetList()
            DashboardAction.OpenBudgetCreate -> openBudgetCreate()
            DashboardAction.OpenSettings -> openSettings()
            is DashboardAction.OpenTransactionEdit -> openTransactionCreate(action.transaction?.id)
            DashboardAction.OpenTransactionList -> openTransactionList()
            DashboardAction.OpenPendingTransactions -> openPendingTransactions()
        }
    }

    private fun openPendingTransactions() {
        appComposeNavigator.navigate(ExpenseManagerScreens.PendingTransactionList)
    }

    companion object {
        private const val MAX_TRANSACTIONS_IN_LIST = 10
    }
}
