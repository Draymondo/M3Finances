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
}
