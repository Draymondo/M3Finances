package com.naveenapps.expensemanager.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.common.utils.toCapitalize
import com.naveenapps.expensemanager.core.domain.usecase.calendar.GetCalendarTransactionsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.CalendarDayData
import com.naveenapps.expensemanager.core.model.CalendarYearMonthData
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarViewModel(
    private val getCalendarTransactionsUseCase: GetCalendarTransactionsUseCase,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
) : ViewModel() {

    private val initialCal = Calendar.getInstance()
    private val currentYear = MutableStateFlow(initialCal.get(Calendar.YEAR))
    private val currentMonth = MutableStateFlow(initialCal.get(Calendar.MONTH) + 1) // 1 to 12
    private val selectedDateFlow = MutableStateFlow(initialCal.time)
    private val viewModeFlow = MutableStateFlow(CalendarViewMode.MONTH)

    private val _state = MutableStateFlow(
        CalendarState(
            year = initialCal.get(Calendar.YEAR),
            month = initialCal.get(Calendar.MONTH) + 1,
            selectedDate = initialCal.time,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.CALENDAR)
        }

        observeCalendarData()
    }

    private data class CalendarNavState(
        val year: Int,
        val month: Int,
        val selectedDate: Date,
        val viewMode: CalendarViewMode,
    )

    private fun observeCalendarData() {
        val navStateFlow = combine(
            currentYear,
            currentMonth,
            selectedDateFlow,
            viewModeFlow,
        ) { year, month, selectedDate, viewMode ->
            CalendarNavState(year, month, selectedDate, viewMode)
        }

        combine(
            navStateFlow,
            getCurrencyUseCase(),
            getCalendarTransactionsUseCase.getAllTransactions(),
        ) { navState, currency, rawTransactions ->
            val year = navState.year
            val month = navState.month
            val selectedDate = navState.selectedDate
            val viewMode = navState.viewMode
            val transactions = rawTransactions.orEmpty()
            val dayFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
            val selectedDayFormatted = dayFormat.format(selectedDate).toCapitalize()

            when (viewMode) {
                CalendarViewMode.DAY -> {
                    val dayCal = Calendar.getInstance().apply { time = selectedDate }
                    val periodTitle = dayFormat.format(selectedDate).toCapitalize()

                    val dayTx = transactions.filter { tx ->
                        val txCal = Calendar.getInstance().apply { time = tx.createdOn }
                        txCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                            txCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                    }

                    val income = dayTx.filter { it.type.isIncome() }.sumOf { it.amount.amount }
                    val expense = dayTx.filter { it.type.isExpense() }.sumOf { it.amount.amount }
                    val net = income - expense

                    val selectedDayTxUi = dayTx.map { tx ->
                        tx.toTransactionUIModel(getFormattedAmountUseCase(tx.amount.amount, currency))
                    }

                    _state.update {
                        it.copy(
                            viewMode = viewMode,
                            year = year,
                            month = month,
                            selectedDate = selectedDate,
                            periodTitle = periodTitle,
                            totalIncome = getFormattedAmountUseCase(income, currency),
                            totalExpense = getFormattedAmountUseCase(expense, currency),
                            netAmount = getFormattedAmountUseCase(net, currency),
                            calendarDays = emptyList(),
                            weekDays = emptyList(),
                            monthsData = emptyList(),
                            selectedDayTransactions = selectedDayTxUi,
                            selectedDayFormatted = selectedDayFormatted,
                            isLoading = false,
                        )
                    }
                }

                CalendarViewMode.WEEK -> {
                    val weekData = getCalendarTransactionsUseCase.buildCalendarWeekData(selectedDate, transactions)
                    val shortDateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                    val periodTitle = "${shortDateFormat.format(weekData.startDate)} – ${shortDateFormat.format(weekData.endDate)} ${yearFormat.format(weekData.endDate)}"

                    val selectedDayData = findDayDataForDate(weekData.days, selectedDate)
                    val selectedDayTxUi = selectedDayData?.transactions?.map { tx ->
                        tx.toTransactionUIModel(getFormattedAmountUseCase(tx.amount.amount, currency))
                    }.orEmpty()

                    _state.update {
                        it.copy(
                            viewMode = viewMode,
                            year = year,
                            month = month,
                            selectedDate = selectedDate,
                            periodTitle = periodTitle,
                            totalIncome = getFormattedAmountUseCase(weekData.totalIncome, currency),
                            totalExpense = getFormattedAmountUseCase(weekData.totalExpense, currency),
                            netAmount = getFormattedAmountUseCase(weekData.netAmount, currency),
                            calendarDays = emptyList(),
                            weekDays = weekData.days,
                            monthsData = emptyList(),
                            selectedDayTransactions = selectedDayTxUi,
                            selectedDayFormatted = selectedDayFormatted,
                            isLoading = false,
                        )
                    }
                }

                CalendarViewMode.MONTH -> {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val periodTitle = monthFormat.format(cal.time).toCapitalize()

                    val monthData = getCalendarTransactionsUseCase.buildCalendarMonthData(year, month, transactions)
                    val selectedDayData = findDayDataForDate(monthData.days, selectedDate)
                    val selectedDayTxUi = selectedDayData?.transactions?.map { tx ->
                        tx.toTransactionUIModel(getFormattedAmountUseCase(tx.amount.amount, currency))
                    }.orEmpty()

                    _state.update {
                        it.copy(
                            viewMode = viewMode,
                            year = year,
                            month = month,
                            selectedDate = selectedDate,
                            periodTitle = periodTitle,
                            totalIncome = getFormattedAmountUseCase(monthData.totalIncome, currency),
                            totalExpense = getFormattedAmountUseCase(monthData.totalExpense, currency),
                            netAmount = getFormattedAmountUseCase(monthData.netAmount, currency),
                            calendarDays = monthData.days,
                            weekDays = emptyList(),
                            monthsData = emptyList(),
                            selectedDayTransactions = selectedDayTxUi,
                            selectedDayFormatted = selectedDayFormatted,
                            isLoading = false,
                        )
                    }
                }

                CalendarViewMode.YEAR -> {
                    val periodTitle = year.toString()
                    val yearData = getCalendarTransactionsUseCase.buildCalendarYearData(year, transactions)

                    _state.update {
                        it.copy(
                            viewMode = viewMode,
                            year = year,
                            month = month,
                            selectedDate = selectedDate,
                            periodTitle = periodTitle,
                            totalIncome = getFormattedAmountUseCase(yearData.totalIncome, currency),
                            totalExpense = getFormattedAmountUseCase(yearData.totalExpense, currency),
                            netAmount = getFormattedAmountUseCase(yearData.netAmount, currency),
                            calendarDays = emptyList(),
                            weekDays = emptyList(),
                            monthsData = yearData.months,
                            selectedDayTransactions = emptyList(),
                            selectedDayFormatted = selectedDayFormatted,
                            isLoading = false,
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun findDayDataForDate(days: List<CalendarDayData>, date: Date): CalendarDayData? {
        val targetCal = Calendar.getInstance().apply { time = date }
        val checkCal = Calendar.getInstance()
        return days.firstOrNull { day ->
            checkCal.time = day.date
            checkCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                checkCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun processAction(action: CalendarAction) {
        when (action) {
            CalendarAction.ClosePage -> appComposeNavigator.popBackStack()
            is CalendarAction.ChangeViewMode -> {
                viewModeFlow.value = action.mode
            }
            CalendarAction.PreviousPeriod -> previousPeriod()
            CalendarAction.NextPeriod -> nextPeriod()
            CalendarAction.GoToToday -> goToToday()
            is CalendarAction.SelectDay -> selectDay(action.date)
            is CalendarAction.SelectMonth -> selectMonth(action.month)
            is CalendarAction.OpenTransaction -> openTransaction(action.transactionId)
            CalendarAction.AddTransaction -> addTransaction()
        }
    }

    private fun previousPeriod() {
        when (viewModeFlow.value) {
            CalendarViewMode.DAY -> {
                val cal = Calendar.getInstance().apply {
                    time = selectedDateFlow.value
                    add(Calendar.DAY_OF_MONTH, -1)
                }
                selectedDateFlow.value = cal.time
                currentYear.value = cal.get(Calendar.YEAR)
                currentMonth.value = cal.get(Calendar.MONTH) + 1
            }
            CalendarViewMode.WEEK -> {
                val cal = Calendar.getInstance().apply {
                    time = selectedDateFlow.value
                    add(Calendar.DAY_OF_MONTH, -7)
                }
                selectedDateFlow.value = cal.time
                currentYear.value = cal.get(Calendar.YEAR)
                currentMonth.value = cal.get(Calendar.MONTH) + 1
            }
            CalendarViewMode.MONTH -> {
                val m = currentMonth.value
                val y = currentYear.value
                if (m == 1) {
                    currentMonth.value = 12
                    currentYear.value = y - 1
                } else {
                    currentMonth.value = m - 1
                }
            }
            CalendarViewMode.YEAR -> {
                currentYear.value = currentYear.value - 1
            }
        }
    }

    private fun nextPeriod() {
        when (viewModeFlow.value) {
            CalendarViewMode.DAY -> {
                val cal = Calendar.getInstance().apply {
                    time = selectedDateFlow.value
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                selectedDateFlow.value = cal.time
                currentYear.value = cal.get(Calendar.YEAR)
                currentMonth.value = cal.get(Calendar.MONTH) + 1
            }
            CalendarViewMode.WEEK -> {
                val cal = Calendar.getInstance().apply {
                    time = selectedDateFlow.value
                    add(Calendar.DAY_OF_MONTH, 7)
                }
                selectedDateFlow.value = cal.time
                currentYear.value = cal.get(Calendar.YEAR)
                currentMonth.value = cal.get(Calendar.MONTH) + 1
            }
            CalendarViewMode.MONTH -> {
                val m = currentMonth.value
                val y = currentYear.value
                if (m == 12) {
                    currentMonth.value = 1
                    currentYear.value = y + 1
                } else {
                    currentMonth.value = m + 1
                }
            }
            CalendarViewMode.YEAR -> {
                currentYear.value = currentYear.value + 1
            }
        }
    }

    private fun goToToday() {
        val today = Calendar.getInstance()
        currentYear.value = today.get(Calendar.YEAR)
        currentMonth.value = today.get(Calendar.MONTH) + 1
        selectedDateFlow.value = today.time
    }

    private fun selectDay(date: Date) {
        selectedDateFlow.value = date
        val cal = Calendar.getInstance().apply { time = date }
        val dYear = cal.get(Calendar.YEAR)
        val dMonth = cal.get(Calendar.MONTH) + 1
        if (dYear != currentYear.value || dMonth != currentMonth.value) {
            currentYear.value = dYear
            currentMonth.value = dMonth
        }
    }

    private fun selectMonth(month: Int) {
        currentMonth.value = month
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear.value)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        selectedDateFlow.value = cal.time
        viewModeFlow.value = CalendarViewMode.MONTH
    }

    private fun openTransaction(transactionId: String) {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(transactionId))
    }

    private fun addTransaction() {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(null))
    }
}
