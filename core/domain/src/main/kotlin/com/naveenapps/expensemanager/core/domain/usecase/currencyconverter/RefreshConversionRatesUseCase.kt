package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository

class RefreshConversionRatesUseCase(private val repository: CurrencyConverterRepository) {

    suspend operator fun invoke(baseCode: String): Resource<CurrencyConversionRates> {
        if (baseCode.isBlank()) {
            return Resource.Error(Exception("Sélectionnez une devise source"))
        }
        return repository.refreshRates(baseCode)
    }
}
