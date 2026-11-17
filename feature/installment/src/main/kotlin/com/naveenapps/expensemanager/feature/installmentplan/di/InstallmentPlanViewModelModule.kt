package com.naveenapps.expensemanager.feature.installmentplan.di

import com.naveenapps.expensemanager.feature.installmentplan.create.InstallmentPlanCreateViewModel
import com.naveenapps.expensemanager.feature.installmentplan.list.InstallmentPlanListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val InstallmentPlanViewModelModule = module {
    viewModel {
        InstallmentPlanListViewModel(
            getInstallmentPlansUseCase = get(),
            trackToolUsageUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        InstallmentPlanCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllAccountsUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            findInstallmentPlanByIdUseCase = get(),
            addInstallmentPlanUseCase = get(),
            updateInstallmentPlanUseCase = get(),
            deleteInstallmentPlanUseCase = get(),
            addInstallmentPaymentUseCase = get(),
            getInstallmentPlanPaymentsUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
