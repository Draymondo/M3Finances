package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import org.koin.dsl.module

val CurrencyConverterUseCaseModule = module {
    single { GetCurrencyConverterApiKeyUseCase(repository = get()) }
    single { SetCurrencyConverterApiKeyUseCase(repository = get()) }
    single { GetCachedConversionRatesUseCase(repository = get()) }
    single { RefreshConversionRatesUseCase(repository = get()) }
    single { ConvertCurrencyUseCase() }
}
