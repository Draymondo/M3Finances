package com.naveenapps.expensemanager.feature.debt.di

import com.naveenapps.expensemanager.feature.debt.create.DebtCreateViewModel
import com.naveenapps.expensemanager.feature.debt.list.DebtListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val DebtViewModelModule = module {
    viewModel {
        DebtListViewModel(
            getDebtsUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        DebtCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllAccountsUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            findDebtByIdUseCase = get(),
            addDebtUseCase = get(),
            updateDebtUseCase = get(),
            deleteDebtUseCase = get(),
            addDebtRepaymentUseCase = get(),
            getDebtRemindersUseCase = get(),
            addDebtReminderUseCase = get(),
            deleteDebtReminderUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
