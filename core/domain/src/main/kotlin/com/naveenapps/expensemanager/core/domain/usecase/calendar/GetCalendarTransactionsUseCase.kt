package com.naveenapps.expensemanager.core.domain.usecase.calendar

import com.naveenapps.expensemanager.core.model.CalendarDayData
import com.naveenapps.expensemanager.core.model.CalendarMonthData
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class GetCalendarTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(year: Int, month: Int): Flow<CalendarMonthData> {
        return transactionRepository.getAllTransaction().map { transactions ->
            buildCalendarMonthData(year, month, transactions.orEmpty())
        }
    }

    fun buildCalendarMonthData(
        year: Int,
        month: Int, // 1 to 12
        transactions: List<Transaction>,
    ): CalendarMonthData {
        val todayCal = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.MONDAY is 2, Calendar.SUNDAY is 1
        // We want Monday = 0, Tuesday = 1, ... Sunday = 6
        val leadingDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        // Map transactions by "yyyy-MM-dd" key
        val txByDayKey = HashMap<String, MutableList<Transaction>>()
        val txCal = Calendar.getInstance()
        for (tx in transactions) {
            txCal.time = tx.createdOn
            val key = "${txCal.get(Calendar.YEAR)}-${txCal.get(Calendar.MONTH)}-${txCal.get(Calendar.DAY_OF_MONTH)}"
            txByDayKey.getOrPut(key) { mutableListOf() }.add(tx)
        }

        val days = mutableListOf<CalendarDayData>()

        // Previous month padding days
        val prevCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadingDays)
        }
        for (i in 0 until leadingDays) {
            val dDate = prevCal.time
            val dYear = prevCal.get(Calendar.YEAR)
            val dMonth = prevCal.get(Calendar.MONTH)
            val dDay = prevCal.get(Calendar.DAY_OF_MONTH)
            val key = "$dYear-$dMonth-$dDay"
            val dayTx = txByDayKey[key].orEmpty()
            val income = dayTx.filter { it.type.isIncome() }.sumOf { it.amount.amount }
            val expense = dayTx.filter { it.type.isExpense() }.sumOf { it.amount.amount }
            val isToday = dYear == todayCal.get(Calendar.YEAR) &&
                dMonth == todayCal.get(Calendar.MONTH) &&
                dDay == todayCal.get(Calendar.DAY_OF_MONTH)

            days.add(
                CalendarDayData(
                    date = dDate,
                    dayOfMonth = dDay,
                    isCurrentMonth = false,
                    isToday = isToday,
                    totalIncome = income,
                    totalExpense = expense,
                    transactions = dayTx,
                ),
            )
            prevCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Current month days
        var monthTotalIncome = 0.0
        var monthTotalExpense = 0.0
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dDate = cal.time
            val key = "$year-${month - 1}-$day"
            val dayTx = txByDayKey[key].orEmpty()
            val income = dayTx.filter { it.type.isIncome() }.sumOf { it.amount.amount }
            val expense = dayTx.filter { it.type.isExpense() }.sumOf { it.amount.amount }
            val isToday = year == todayCal.get(Calendar.YEAR) &&
                (month - 1) == todayCal.get(Calendar.MONTH) &&
                day == todayCal.get(Calendar.DAY_OF_MONTH)

            monthTotalIncome += income
            monthTotalExpense += expense

            days.add(
                CalendarDayData(
                    date = dDate,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = isToday,
                    totalIncome = income,
                    totalExpense = expense,
                    transactions = dayTx,
                ),
            )
        }

        // Next month padding days to complete grid (multiples of 7)
        val remainder = days.size % 7
        val trailingDays = if (remainder != 0) 7 - remainder else 0
        val nextCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        for (i in 0 until trailingDays) {
            val dDate = nextCal.time
            val dYear = nextCal.get(Calendar.YEAR)
            val dMonth = nextCal.get(Calendar.MONTH)
            val dDay = nextCal.get(Calendar.DAY_OF_MONTH)
            val key = "$dYear-$dMonth-$dDay"
            val dayTx = txByDayKey[key].orEmpty()
            val income = dayTx.filter { it.type.isIncome() }.sumOf { it.amount.amount }
            val expense = dayTx.filter { it.type.isExpense() }.sumOf { it.amount.amount }
            val isToday = dYear == todayCal.get(Calendar.YEAR) &&
                dMonth == todayCal.get(Calendar.MONTH) &&
                dDay == todayCal.get(Calendar.DAY_OF_MONTH)

            days.add(
                CalendarDayData(
                    date = dDate,
                    dayOfMonth = dDay,
                    isCurrentMonth = false,
                    isToday = isToday,
                    totalIncome = income,
                    totalExpense = expense,
                    transactions = dayTx,
                ),
            )
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return CalendarMonthData(
            year = year,
            month = month,
            totalIncome = monthTotalIncome,
            totalExpense = monthTotalExpense,
            netAmount = monthTotalIncome - monthTotalExpense,
            days = days,
        )
    }
}

