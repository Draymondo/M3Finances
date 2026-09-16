package com.naveenapps.expensemanager.core.model

import java.util.Date

data class CalendarDayData(
    val date: Date,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
) {
    val netAmount: Double
        get() = totalIncome - totalExpense
}

data class CalendarMonthData(
    val year: Int,
    val month: Int, // 1 to 12
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netAmount: Double = 0.0,
    val days: List<CalendarDayData> = emptyList(),
)

