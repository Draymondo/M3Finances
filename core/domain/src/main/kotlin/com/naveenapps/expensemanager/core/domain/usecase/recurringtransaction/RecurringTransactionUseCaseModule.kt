package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import org.koin.dsl.module

val RecurringTransactionUseCaseModule = module {
    single { AddRecurringTransactionUseCase(repository = get()) }
    single { UpdateRecurringTransactionUseCase(repository = get()) }
    single { DeleteRecurringTransactionUseCase(repository = get()) }
    single { GetAllRecurringTransactionUseCase(repository = get()) }
    single { FindRecurringTransactionByIdUseCase(repository = get()) }
    single {
        GetRecurringTransactionsUseCase(
            repository = get(),
            getAllAccountsUseCase = get(),
            getAllCategoryUseCase = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            appCoroutineDispatchers = get(),
        )
    }
    single {
        ProcessDueRecurringTransactionsUseCase(
            repository = get(),
            addTransactionUseCase = get(),
        )
    }
}
