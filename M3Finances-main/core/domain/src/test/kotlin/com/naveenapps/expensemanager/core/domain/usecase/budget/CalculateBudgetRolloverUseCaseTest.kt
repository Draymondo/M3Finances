package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date

class CalculateBudgetRolloverUseCaseTest : BaseCoroutineTest() {

    private val budgetRepository: BudgetRepository = mock()
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase = mock()

    private lateinit var calculateBudgetRolloverUseCase: CalculateBudgetRolloverUseCase

    override fun onCreate() {
        super.onCreate()
        calculateBudgetRolloverUseCase = CalculateBudgetRolloverUseCase(
            budgetRepository = budgetRepository,
            getBudgetTransactionsUseCase = getBudgetTransactionsUseCase,
        )
    }

    @Test
    fun whenPreviousPeriodHasSavingsShouldReturnCorrectRollover() = runTest {
        val julyBudget = Budget(
            id = "july_id",
            amount = 500.0,
            selectedMonth = "July 2026",
            periodType = BudgetPeriod.MONTHLY,
            categories = listOf("cat_1"),
            accounts = listOf("acc_1"),
            isAllAccountsSelected = false,
            isAllCategoriesSelected = false,
            createdOn = Date(),
            updatedOn = Date(),
        )

        val augustBudget = Budget(
            id = "august_id",
            amount = 500.0,
            selectedMonth = "August 2026",
            periodType = BudgetPeriod.MONTHLY,
            categories = listOf("cat_1"),
            accounts = listOf("acc_1"),
            isAllAccountsSelected = false,
            isAllCategoriesSelected = false,
            createdOn = Date(),
            updatedOn = Date(),
        )

        whenever(budgetRepository.getBudgets()).thenReturn(flowOf(listOf(julyBudget, augustBudget)))

        val transactions = listOf(
            Transaction(
                id = "t1",
                notes = "notes",
                categoryId = "cat_1",
                fromAccountId = "acc_1",
                toAccountId = null,
                type = TransactionType.EXPENSE,
                amount = Amount(400.0),
                imagePath = "",
                createdOn = Date(),
                updatedOn = Date(),
            )
        )
        whenever(getBudgetTransactionsUseCase.invoke(julyBudget)).thenReturn(Resource.Success(transactions))

        val rollover = calculateBudgetRolloverUseCase.invoke(augustBudget)
        Truth.assertThat(rollover).isEqualTo(100.0) // 500 - 400 = 100 unspent
    }

    @Test
    fun whenPreviousPeriodIsOverspentShouldReturnZeroRollover() = runTest {
        val julyBudget = Budget(
            id = "july_id",
            amount = 500.0,
            selectedMonth = "July 2026",
            periodType = BudgetPeriod.MONTHLY,
            categories = listOf("cat_1"),
            accounts = listOf("acc_1"),
            isAllAccountsSelected = false,
            isAllCategoriesSelected = false,
            createdOn = Date(),
            updatedOn = Date(),
        )

        val augustBudget = Budget(
            id = "august_id",
            amount = 500.0,
            selectedMonth = "August 2026",
            periodType = BudgetPeriod.MONTHLY,
            categories = listOf("cat_1"),
            accounts = listOf("acc_1"),
            isAllAccountsSelected = false,
            isAllCategoriesSelected = false,
            createdOn = Date(),
            updatedOn = Date(),
        )

        whenever(budgetRepository.getBudgets()).thenReturn(flowOf(listOf(julyBudget, augustBudget)))

        val transactions = listOf(
            Transaction(
                id = "t1",
                notes = "notes",
                categoryId = "cat_1",
                fromAccountId = "acc_1",
                toAccountId = null,
                type = TransactionType.EXPENSE,
                amount = Amount(600.0),
                imagePath = "",
                createdOn = Date(),
                updatedOn = Date(),
            )
        )
        whenever(getBudgetTransactionsUseCase.invoke(julyBudget)).thenReturn(Resource.Success(transactions))

        val rollover = calculateBudgetRolloverUseCase.invoke(augustBudget)
        Truth.assertThat(rollover).isEqualTo(0.0) // 500 - 600 = -100 (overspent, capped at 0)
    }
}

