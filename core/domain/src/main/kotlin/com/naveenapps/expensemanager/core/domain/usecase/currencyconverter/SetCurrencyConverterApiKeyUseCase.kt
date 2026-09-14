package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository

class SetCurrencyConverterApiKeyUseCase(private val repository: CurrencyConverterRepository) {

    suspend operator fun invoke(apiKey: String): Resource<Boolean> {
        if (apiKey.isBlank()) {
            return Resource.Error(Exception("La clé API ne peut pas être vide"))
        }
        repository.setApiKey(apiKey.trim())
        return Resource.Success(true)
    }
}
