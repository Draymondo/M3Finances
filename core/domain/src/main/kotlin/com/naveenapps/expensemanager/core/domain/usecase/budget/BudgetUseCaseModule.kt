package com.naveenapps.expensemanager.core.domain.usecase.budget

import org.koin.dsl.module

val BudgetUseCaseModule = module {
    single {
        AddBudgetUseCase(
            repository = get(),
            checkBudgetValidateUseCase = get()
        )
    }
    single { CheckBudgetValidateUseCase() }
    single {
        DeleteBudgetUseCase(
            repository = get(),
            checkBudgetValidateUseCase = get()
        )
    }
    single { FindBudgetByIdUseCase(repository = get()) }
    single {
        GetBudgetDetailUseCase(
            budgetRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            getBudgetTransactionsUseCase = get(),
            getTransactionWithFilterUseCase = get(),
            getBudgetEquivalentsUseCase = get(),
            calculateBudgetRolloverUseCase = get(),
        )
    }
    single {
        GetBudgetEquivalentsUseCase(
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
        )
    }
    single {
        GetBudgetTransactionsUseCase(
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get()
        )
    }
    single {
        GetBudgetsUseCase(
            budgetRepository = get(),
            getTransactionWithFilterUseCase = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            getBudgetTransactionsUseCase = get(),
            calculateBudgetRolloverUseCase = get(),
            appCoroutineDispatchers = get()
        )
    }
    single {
        UpdateBudgetUseCase(
            repository = get(),
            checkBudgetValidateUseCase = get()
        )
    }
    single {
        CalculateBudgetRolloverUseCase(
            budgetRepository = get(),
            getBudgetTransactionsUseCase = get(),
        )
    }
    single { GetRequiredIncomeUseCase(get(), get(), get(), get(), get(), get()) }
}

