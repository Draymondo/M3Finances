package com.naveenapps.expensemanager.core.domain.usecase.networth

import org.koin.dsl.module

val NetWorthUseCaseModule = module {
    single {
        GetNetWorthChartDataUseCase(
            accountRepository = get(),
            transactionRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            getDateRangeUseCase = get(),
            getTransactionGroupTypeUseCase = get(),
            dispatcher = get(),
        )
    }
}
