package com.naveenapps.expensemanager.core.domain.usecase.calendar

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date

class GetCalendarTransactionsUseCaseTest : BaseCoroutineTest() {

    private val transactionRepository: TransactionRepository = mock()
    private lateinit var useCase: GetCalendarTransactionsUseCase

    override fun onCreate() {
        super.onCreate()
        useCase = GetCalendarTransactionsUseCase(transactionRepository)
    }

    private fun createTransaction(
        id: String,
        amount: Double,
        type: TransactionType,
        year: Int,
        month: Int, // 1-12
        day: Int,
    ): Transaction {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return Transaction(
            id = id,
            notes = "Test note",
            categoryId = "cat-1",
            fromAccountId = "acc-1",
            toAccountId = null,
            amount = Amount(amount, amount.toString()),
            imagePath = "",
            type = type,
            createdOn = cal.time,
            updatedOn = cal.time,
            category = Category("cat-1", "Test", CategoryType.EXPENSE, StoredIcon("", ""), Date(), Date()),
        )
    }

    @Test
    fun `test July 2026 calendar grid dimensions and leading days`() = runTest {
        whenever(transactionRepository.getAllTransaction()).thenReturn(flowOf(emptyList()))

        val data = useCase(2026, 7).first()

        assertThat(data.year).isEqualTo(2026)
        assertThat(data.month).isEqualTo(7)
        // July 1, 2026 is Wednesday. Monday-start grid has 2 leading days (Monday June 29, Tuesday June 30)
        // July has 31 days. 2 + 31 = 33 days. 2 trailing days (Aug 1, Aug 2). Total = 35 days (multiple of 7).
        assertThat(data.days.size).isEqualTo(35)
        assertThat(data.days.size % 7).isEqualTo(0)

        // First cell is June 29 (not current month)
        assertThat(data.days[0].isCurrentMonth).isFalse()
        assertThat(data.days[0].dayOfMonth).isEqualTo(29)

        // Third cell is July 1 (current month)
        assertThat(data.days[2].isCurrentMonth).isTrue()
        assertThat(data.days[2].dayOfMonth).isEqualTo(1)

        // 33rd cell is July 31
        assertThat(data.days[32].isCurrentMonth).isTrue()
        assertThat(data.days[32].dayOfMonth).isEqualTo(31)

        // 34th cell is August 1
        assertThat(data.days[33].isCurrentMonth).isFalse()
        assertThat(data.days[33].dayOfMonth).isEqualTo(1)
    }

    @Test
    fun `test financial aggregations on daily and monthly levels`() = runTest {
        val txList = listOf(
            createTransaction("1", 4800.0, TransactionType.INCOME, 2026, 7, 1),
            createTransaction("2", 1350.0, TransactionType.EXPENSE, 2026, 7, 2),
            createTransaction("3", 950.0, TransactionType.INCOME, 2026, 7, 10),
            createTransaction("4", 55.0, TransactionType.EXPENSE, 2026, 7, 10),
            createTransaction("5", 500.0, TransactionType.EXPENSE, 2026, 8, 1), // Next month, should not count in July totals
        )
        whenever(transactionRepository.getAllTransaction()).thenReturn(flowOf(txList))

        val data = useCase(2026, 7).first()

        // Month totals (only July transactions)
        assertThat(data.totalIncome).isEqualTo(4800.0 + 950.0) // 5750.0
        assertThat(data.totalExpense).isEqualTo(1350.0 + 55.0) // 1405.0
        assertThat(data.netAmount).isEqualTo(5750.0 - 1405.0) // 4345.0

        // Day 10 (July 10)
        val day10 = data.days.first { it.isCurrentMonth && it.dayOfMonth == 10 }
        assertThat(day10.totalIncome).isEqualTo(950.0)
        assertThat(day10.totalExpense).isEqualTo(55.0)
        assertThat(day10.netAmount).isEqualTo(895.0)
        assertThat(day10.transactions.size).isEqualTo(2)
    }
}

