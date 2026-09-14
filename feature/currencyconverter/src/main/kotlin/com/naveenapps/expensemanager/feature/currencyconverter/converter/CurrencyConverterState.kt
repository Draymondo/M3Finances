package com.naveenapps.expensemanager.feature.currencyconverter.converter

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Country
import com.naveenapps.expensemanager.core.model.TextFieldValue
import java.util.Date

@Stable
data class CurrencyConverterState(
    val amount: TextFieldValue<String>,
    val fromCurrency: Country? = null,
    val toCurrency: Country? = null,
    val result: String? = null,
    val lastUpdated: Date? = null,
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = true,
    val errorMessage: String? = null,
    val showFromCurrencySelection: Boolean = false,
    val showToCurrencySelection: Boolean = false,
)
