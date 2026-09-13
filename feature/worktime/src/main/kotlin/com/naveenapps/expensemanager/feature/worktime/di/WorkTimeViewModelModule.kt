package com.naveenapps.expensemanager.feature.worktime.di

import com.naveenapps.expensemanager.feature.worktime.calculator.WorkTimeCalculatorViewModel
import com.naveenapps.expensemanager.feature.worktime.settings.WorkTimeSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val WorkTimeViewModelModule = module {
    viewModel {
        WorkTimeCalculatorViewModel(
            getWorkTimeSettingsUseCase = get(),
            calculateWorkTimeEquivalentUseCase = get(),
            setWorkTimeAutoDetectEnabledUseCase = get(),
            numberFormatRepository = get(),
            appComposeNavigator = get(),
            trackToolUsageUseCase = get(),
        )
    }
    viewModel {
        WorkTimeSettingsViewModel(
            getWorkTimeSettingsUseCase = get(),
            getAllCategoryUseCase = get(),
            updateWorkTimeSettingsUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
