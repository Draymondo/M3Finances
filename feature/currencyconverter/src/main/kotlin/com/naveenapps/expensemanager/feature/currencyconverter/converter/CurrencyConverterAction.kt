package com.naveenapps.expensemanager.feature.currencyconverter.converter

import com.naveenapps.expensemanager.core.model.Country

sealed class CurrencyConverterAction {

    data object ClosePage : CurrencyConverterAction()

    data object OpenSettings : CurrencyConverterAction()

    data object ShowFromCurrencySelection : CurrencyConverterAction()

    data object ShowToCurrencySelection : CurrencyConverterAction()

    data object DismissCurrencySelection : CurrencyConverterAction()

    data class SelectFromCurrency(val country: Country) : CurrencyConverterAction()

    data class SelectToCurrency(val country: Country) : CurrencyConverterAction()

    data object SwapCurrencies : CurrencyConverterAction()

    data object Refresh : CurrencyConverterAction()
}
