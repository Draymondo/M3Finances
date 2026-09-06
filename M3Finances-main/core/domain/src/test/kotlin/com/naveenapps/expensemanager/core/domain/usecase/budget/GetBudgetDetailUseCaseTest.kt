package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.repository.CurrencyRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import org.junit.Test
import org.mockito.kotlin.mock

import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase

class GetBudgetDetailUseCaseTest : BaseCoroutineTest() {

    private val budgetRepository: BudgetRepository = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val currencyRepository: CurrencyRepository = mock()
    private val calculateBudgetRolloverUseCase: CalculateBudgetRolloverUseCase = mock()
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase = mock()
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase = mock()
    private val getBudgetEquivalentsUseCase: GetBudgetEquivalentsUseCase = mock()

    private val getCurrencyUseCase = GetCurrencyUseCase(currencyRepository)
    private val getFormattedAmountUseCase = GetFormattedAmountUseCase(currencyRepository)

    private lateinit var getBudgetDetailUseCase: GetBudgetDetailUseCase

    override fun onCreate() {
        super.onCreate()
        getBudgetDetailUseCase = GetBudgetDetailUseCase(
            budgetRepository = budgetRepository,
            getCurrencyUseCase = getCurrencyUseCase,
            getFormattedAmountUseCase = getFormattedAmountUseCase,
            getBudgetTransactionsUseCase = getBudgetTransactionsUseCase,
            getTransactionWithFilterUseCase = getTransactionWithFilterUseCase,
            getBudgetEquivalentsUseCase = getBudgetEquivalentsUseCase,
            calculateBudgetRolloverUseCase = calculateBudgetRolloverUseCase,
        )
    }

    @Test
    fun whenAllDataAvailableItShouldSendSuccessResult() {
    }

    @Test
    fun whenBudgetNameIsProvidedItShouldBeUsed() {
        val budget = com.naveenapps.expensemanager.core.testing.FAKE_BUDGET.copy(name = "Courses du mois")
        val uiModel = budget.toBudgetUiModel(
            budgetAmount = com.naveenapps.expensemanager.core.model.Amount(500.0),
            transactionAmount = com.naveenapps.expensemanager.core.model.Amount(100.0),
            percent = 20f,
        )
        com.google.common.truth.Truth.assertThat(uiModel.name).isEqualTo("Courses du mois")
    }

    @Test
    fun whenBudgetNameIsNullItShouldFallbackToBudgetName() {
        val budget = com.naveenapps.expensemanager.core.testing.FAKE_BUDGET.copy(
            name = null,
            periodType = com.naveenapps.expensemanager.core.model.BudgetPeriod.MONTHLY,
        )
        val uiModel = budget.toBudgetUiModel(
            budgetAmount = com.naveenapps.expensemanager.core.model.Amount(500.0),
            transactionAmount = com.naveenapps.expensemanager.core.model.Amount(100.0),
            percent = 20f,
        )
        com.google.common.truth.Truth.assertThat(uiModel.name)
            .isEqualTo(budgetName(budget.selectedMonth, budget.periodType))
    }

    @Test
    fun whenBudgetNameIsBlankItShouldFallbackToBudgetName() {
        val budget = com.naveenapps.expensemanager.core.testing.FAKE_BUDGET.copy(
            name = "   ",
            periodType = com.naveenapps.expensemanager.core.model.BudgetPeriod.MONTHLY,
        )
        val uiModel = budget.toBudgetUiModel(
            budgetAmount = com.naveenapps.expensemanager.core.model.Amount(500.0),
            transactionAmount = com.naveenapps.expensemanager.core.model.Amount(100.0),
            percent = 20f,
        )
        com.google.common.truth.Truth.assertThat(uiModel.name)
            .isEqualTo(budgetName(budget.selectedMonth, budget.periodType))
    }
}
