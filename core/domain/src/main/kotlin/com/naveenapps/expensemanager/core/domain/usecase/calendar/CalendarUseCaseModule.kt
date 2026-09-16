package com.naveenapps.expensemanager.core.domain.usecase.calendar

import org.koin.dsl.module

val CalendarUseCaseModule = module {
    factory { GetCalendarTransactionsUseCase(get()) }
}

