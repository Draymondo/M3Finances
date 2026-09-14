package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository
import kotlinx.coroutines.flow.Flow

class GetCurrencyConverterApiKeyUseCase(private val repository: CurrencyConverterRepository) {
    operator fun invoke(): Flow<String?> = repository.getApiKey()
}
