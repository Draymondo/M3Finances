package com.naveenapps.expensemanager.feature.calendar

import java.util.Date

sealed class CalendarAction {
    data object ClosePage : CalendarAction()
    data class ChangeViewMode(val mode: CalendarViewMode) : CalendarAction()
    data object PreviousPeriod : CalendarAction()
    data object NextPeriod : CalendarAction()
    data object GoToToday : CalendarAction()
    data class SelectDay(val date: Date) : CalendarAction()
    data class OpenTransaction(val transactionId: String) : CalendarAction()
    data object AddTransaction : CalendarAction()
}

