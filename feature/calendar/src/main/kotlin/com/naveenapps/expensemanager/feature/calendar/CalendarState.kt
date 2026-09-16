package com.naveenapps.expensemanager.feature.calendar

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.CalendarDayData
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import java.util.Date

enum class CalendarViewMode {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

@Stable
data class CalendarState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val year: Int = 2026,
    val month: Int = 7, // 1 to 12
    val selectedDate: Date = Date(),
    val periodTitle: String = "",
    val totalIncome: Amount = Amount(0.0),
    val totalExpense: Amount = Amount(0.0),
    val netAmount: Amount = Amount(0.0),
    val calendarDays: List<CalendarDayData> = emptyList(),
    val selectedDayTransactions: List<TransactionUiItem> = emptyList(),
    val selectedDayFormatted: String = "",
    val isLoading: Boolean = false,
)

