package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import org.koin.dsl.module

val SavingsGoalUseCaseModule = module {
    single {
        AddSavingsGoalUseCase(
            savingsGoalRepository = get(),
            accountRepository = get(),
            addTransactionUseCase = get(),
            getAllCategoryUseCase = get(),
        )
    }
    single {
        AddSavingsGoalContributionUseCase(
            addTransactionUseCase = get(),
            getAllCategoryUseCase = get(),
        )
    }
    single { UpdateSavingsGoalUseCase(repository = get()) }
    single { DeleteSavingsGoalUseCase(accountRepository = get()) }
    single { FindSavingsGoalByIdUseCase(repository = get()) }
    single {
        GetSavingsGoalsUseCase(
            repository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            recalculateGoalEstimateUseCase = get(),
            appCoroutineDispatchers = get(),
        )
    }
    single { RecalculateGoalEstimateUseCase(transactionRepository = get()) }
    single { SuggestContributionUseCase(savingsGoalRepository = get()) }
}
