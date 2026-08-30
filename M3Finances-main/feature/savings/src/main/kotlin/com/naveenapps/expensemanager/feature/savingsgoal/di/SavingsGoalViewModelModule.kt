package com.naveenapps.expensemanager.feature.savingsgoal.di

import com.naveenapps.expensemanager.feature.savingsgoal.create.SavingsGoalCreateViewModel
import com.naveenapps.expensemanager.feature.savingsgoal.list.SavingsGoalListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val SavingsGoalViewModelModule = module {
    viewModel {
        SavingsGoalListViewModel(
            getSavingsGoalsUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        SavingsGoalCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllAccountsUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            findSavingsGoalByIdUseCase = get(),
            addSavingsGoalUseCase = get(),
            updateSavingsGoalUseCase = get(),
            deleteSavingsGoalUseCase = get(),
            addSavingsGoalContributionUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
