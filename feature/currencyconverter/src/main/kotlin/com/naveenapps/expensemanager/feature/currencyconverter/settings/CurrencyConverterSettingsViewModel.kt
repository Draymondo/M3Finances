package com.naveenapps.expensemanager.feature.currencyconverter.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.GetCurrencyConverterApiKeyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.SetCurrencyConverterApiKeyUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CurrencyConverterSettingsViewModel(
    private val getCurrencyConverterApiKeyUseCase: GetCurrencyConverterApiKeyUseCase,
    private val setCurrencyConverterApiKeyUseCase: SetCurrencyConverterApiKeyUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CurrencyConverterSettingsState(
            apiKey = TextFieldValue(value = "", valueError = false, onValueChange = this::setApiKey),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currentKey = getCurrencyConverterApiKeyUseCase().first()
            if (!currentKey.isNullOrBlank()) {
                _state.update { it.copy(apiKey = it.apiKey.copy(value = currentKey)) }
            }
        }
    }

    private fun setApiKey(value: String) {
        _state.update {
            it.copy(apiKey = it.apiKey.copy(value = value, valueError = false), errorMessage = null)
        }
    }

    private fun save() {
        viewModelScope.launch {
            when (val response = setCurrencyConverterApiKeyUseCase(_state.value.apiKey.value)) {
                is Resource.Error -> _state.update {
                    it.copy(
                        apiKey = it.apiKey.copy(valueError = true),
                        errorMessage = response.exception.message,
                    )
                }

                is Resource.Success -> closePage()
            }
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: CurrencyConverterSettingsAction) {
        when (action) {
            CurrencyConverterSettingsAction.ClosePage -> closePage()
            CurrencyConverterSettingsAction.Save -> save()
        }
    }
}
