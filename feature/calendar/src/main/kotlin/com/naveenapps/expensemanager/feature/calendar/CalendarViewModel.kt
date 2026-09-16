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
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val currentMonth = MutableStateFlow(initialCal.get(Calendar.MONTH) + 1) // 1-12
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCalendarData() {
        combine(
            currentYear,
            currentMonth,
            getCurrencyUseCase(),
        ) { year, month, currency ->
            Triple(year, month, currency)
        }.flatMapLatest { (year, month, currency) ->
            combine(
                getCalendarTransactionsUseCase(year, month),
                selectedDateFlow,
                viewModeFlow,
            ) { monthData, selectedDate, viewMode ->
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val periodTitle = monthFormat.format(cal.time).toCapitalize()

                val formattedIncome = getFormattedAmountUseCase(monthData.totalIncome, currency)
                val formattedExpense = getFormattedAmountUseCase(monthData.totalExpense, currency)
                val formattedNet = getFormattedAmountUseCase(monthData.netAmount, currency)

                val selectedDayData = findDayDataForDate(monthData.days, selectedDate)
                val selectedDayTxUi = selectedDayData?.transactions?.map { tx ->
                    tx.toTransactionUIModel(getFormattedAmountUseCase(tx.amount.amount, currency))
                }.orEmpty()

                val dayFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
                val selectedDayFormatted = dayFormat.format(selectedDate).toCapitalize()

                _state.update {
                    it.copy(
                        viewMode = viewMode,
                        year = year,
                        month = month,
                        selectedDate = selectedDate,
                        periodTitle = periodTitle,
                        totalIncome = formattedIncome,
                        totalExpense = formattedExpense,
                        netAmount = formattedNet,
                        calendarDays = monthData.days,
                        selectedDayTransactions = selectedDayTxUi,
                        selectedDayFormatted = selectedDayFormatted,
                        isLoading = false,
                    )
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
            is CalendarAction.OpenTransaction -> openTransaction(action.transactionId)
            CalendarAction.AddTransaction -> addTransaction()
        }
    }

    private fun previousPeriod() {
        val m = currentMonth.value
        val y = currentYear.value
        if (m == 1) {
            currentMonth.value = 12
            currentYear.value = y - 1
        } else {
            currentMonth.value = m - 1
        }
    }

    private fun nextPeriod() {
        val m = currentMonth.value
        val y = currentYear.value
        if (m == 12) {
            currentMonth.value = 1
            currentYear.value = y + 1
        } else {
            currentMonth.value = m + 1
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

    private fun openTransaction(transactionId: String) {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(transactionId))
    }

    private fun addTransaction() {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(null))
    }
}

