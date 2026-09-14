package com.naveenapps.expensemanager.feature.currencyconverter.di

import com.naveenapps.expensemanager.feature.currencyconverter.converter.CurrencyConverterViewModel
import com.naveenapps.expensemanager.feature.currencyconverter.settings.CurrencyConverterSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val CurrencyConverterViewModelModule = module {
    viewModel {
        CurrencyConverterViewModel(
            getCurrencyConverterApiKeyUseCase = get(),
            refreshConversionRatesUseCase = get(),
            getCachedConversionRatesUseCase = get(),
            convertCurrencyUseCase = get(),
            numberFormatRepository = get(),
            appComposeNavigator = get(),
            trackToolUsageUseCase = get(),
        )
    }

    viewModel {
        CurrencyConverterSettingsViewModel(
            getCurrencyConverterApiKeyUseCase = get(),
            setCurrencyConverterApiKeyUseCase = get(),
            appComposeNavigator = get(),
        )
    }
}
