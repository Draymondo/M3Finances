package com.naveenapps.expensemanager.feature.currencyconverter.settings

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.TextFieldValue

@Stable
data class CurrencyConverterSettingsState(
    val apiKey: TextFieldValue<String>,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)
