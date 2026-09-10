package com.naveenapps.expensemanager.feature.recurring.di

import com.naveenapps.expensemanager.feature.recurring.create.RecurringTransactionCreateViewModel
import com.naveenapps.expensemanager.feature.recurring.list.RecurringTransactionListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RecurringViewModelModule = module {
    viewModel {
        RecurringTransactionListViewModel(
            getRecurringTransactionsUseCase = get(),
            trackToolUsageUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        RecurringTransactionCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllAccountsUseCase = get(),
            getAllCategoryUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            findRecurringTransactionByIdUseCase = get(),
            addRecurringTransactionUseCase = get(),
            updateRecurringTransactionUseCase = get(),
            deleteRecurringTransactionUseCase = get(),
            settingsRepository = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
