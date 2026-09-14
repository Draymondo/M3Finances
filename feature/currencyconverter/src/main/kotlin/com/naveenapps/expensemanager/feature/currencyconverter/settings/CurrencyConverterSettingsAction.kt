package com.naveenapps.expensemanager.feature.currencyconverter.settings

sealed class CurrencyConverterSettingsAction {

    data object ClosePage : CurrencyConverterSettingsAction()

    data object Save : CurrencyConverterSettingsAction()
}
