package com.naveenapps.expensemanager.feature.calendar.di

import com.naveenapps.expensemanager.feature.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val CalendarViewModelModule = module {
    viewModel {
        CalendarViewModel(
            getCalendarTransactionsUseCase = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            appComposeNavigator = get(),
            trackToolUsageUseCase = get(),
        )
    }
}

