package com.naveenapps.expensemanager.core.domain.usecase.debt

import org.koin.dsl.module

val DebtUseCaseModule = module {
    single {
        AddDebtUseCase(
            debtRepository = get(),
            accountRepository = get(),
            addTransactionUseCase = get(),
            getAllCategoryUseCase = get(),
        )
    }
    single {
        AddDebtRepaymentUseCase(
            addTransactionUseCase = get(),
            getAllCategoryUseCase = get(),
        )
    }
    single { UpdateDebtUseCase(repository = get()) }
    single { DeleteDebtUseCase(accountRepository = get()) }
    single { FindDebtByIdUseCase(repository = get()) }
    single { AddDebtReminderUseCase(repository = get()) }
    single { DeleteDebtReminderUseCase(repository = get()) }
    single { GetDebtRemindersUseCase(repository = get()) }
    single {
        GetDebtsUseCase(
            repository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            appCoroutineDispatchers = get(),
        )
    }
}
