package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository
import kotlinx.coroutines.flow.Flow

class GetCachedConversionRatesUseCase(private val repository: CurrencyConverterRepository) {
    operator fun invoke(baseCode: String): Flow<CurrencyConversionRates?> =
        repository.getCachedRates(baseCode)
}
