package com.naveenapps.expensemanager.feature.currencyconverter.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.ConvertCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.GetCachedConversionRatesUseCase
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.GetCurrencyConverterApiKeyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.RefreshConversionRatesUseCase
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.model.Country
import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Aucune devise par défaut n'est imposée — [CurrencyConverterState.fromCurrency] et
 * [CurrencyConverterState.toCurrency] démarrent à `null`, l'utilisateur choisit à chaque
 * ouverture de l'outil (voir AGENTS_currency_converter.md). Le rafraîchissement des taux se
 * déclenche automatiquement dès que la devise source est choisie ou changée — un seul appel
 * réseau par devise source, puisque `conversion_rates` contient déjà toutes les devises cibles.
 */
class CurrencyConverterViewModel(
    getCurrencyConverterApiKeyUseCase: GetCurrencyConverterApiKeyUseCase,
    private val refreshConversionRatesUseCase: RefreshConversionRatesUseCase,
    private val getCachedConversionRatesUseCase: GetCachedConversionRatesUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val numberFormatRepository: NumberFormatRepository,
    private val appComposeNavigator: AppComposeNavigator,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CurrencyConverterState(
            amount = TextFieldValue(value = "", valueError = false, onValueChange = this::setAmount),
        ),
    )
    val state = _state.asStateFlow()

    private var currentRates: CurrencyConversionRates? = null

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.CURRENCY_CONVERTER)
        }
        getCurrencyConverterApiKeyUseCase().onEach { apiKey ->
            _state.update { it.copy(hasApiKey = !apiKey.isNullOrBlank()) }
        }.launchIn(viewModelScope)
    }

    private fun setAmount(value: String) {
        _state.update {
            it.copy(amount = it.amount.copy(value = value, valueError = false))
        }
        recomputeResult()
    }

    private fun selectFromCurrency(country: Country) {
        _state.update {
            it.copy(
                fromCurrency = country,
                showFromCurrencySelection = false,
                result = null,
                errorMessage = null,
            )
        }
        refreshRates(country.currencyCode)
    }

    private fun selectToCurrency(country: Country) {
        _state.update { it.copy(toCurrency = country, showToCurrencySelection = false) }
        recomputeResult()
    }

    private fun swapCurrencies() {
        val current = _state.value
        val newFrom = current.toCurrency
        val newTo = current.fromCurrency
        _state.update {
            it.copy(fromCurrency = newFrom, toCurrency = newTo, result = null, errorMessage = null)
        }
        if (newFrom != null) {
            refreshRates(newFrom.currencyCode)
        } else {
            currentRates = null
        }
    }

    private fun refresh() {
        val from = _state.value.fromCurrency ?: return
        refreshRates(from.currencyCode)
    }

    private fun refreshRates(baseCode: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val response = refreshConversionRatesUseCase(baseCode)) {
                is Resource.Success -> {
                    currentRates = response.data
                    _state.update {
                        it.copy(isLoading = false, lastUpdated = response.data.lastUpdated)
                    }
                    recomputeResult()
                }

                is Resource.Error -> {
                    val cached = getCachedConversionRatesUseCase(baseCode).first()
                    if (cached != null) {
                        currentRates = cached
                        _state.update {
                            it.copy(
                                isLoading = false,
                                lastUpdated = cached.lastUpdated,
                                errorMessage = "Actualisation impossible — derniers taux connus affichés",
                            )
                        }
                        recomputeResult()
                    } else {
                        currentRates = null
                        _state.update {
                            it.copy(isLoading = false, errorMessage = response.exception.message)
                        }
                    }
                }
            }
        }
    }

    private fun recomputeResult() {
        val rates = currentRates ?: return
        val toCurrency = _state.value.toCurrency ?: return
        val amountValue = numberFormatRepository.parseToDouble(_state.value.amount.value)
        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(result = null) }
            return
        }
        when (val response = convertCurrencyUseCase(amountValue, toCurrency.currencyCode, rates)) {
            is Resource.Success -> _state.update {
                it.copy(result = numberFormatRepository.formatForDisplay(response.data))
            }

            is Resource.Error -> _state.update {
                it.copy(result = null, errorMessage = response.exception.message)
            }
        }
    }

    private fun openSettings() {
        appComposeNavigator.navigate(ExpenseManagerScreens.CurrencyConverterSettings)
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: CurrencyConverterAction) {
        when (action) {
            CurrencyConverterAction.ClosePage -> closePage()
            CurrencyConverterAction.OpenSettings -> openSettings()
            CurrencyConverterAction.ShowFromCurrencySelection ->
                _state.update { it.copy(showFromCurrencySelection = true) }

            CurrencyConverterAction.ShowToCurrencySelection ->
                _state.update { it.copy(showToCurrencySelection = true) }

            CurrencyConverterAction.DismissCurrencySelection -> _state.update {
                it.copy(showFromCurrencySelection = false, showToCurrencySelection = false)
            }

            is CurrencyConverterAction.SelectFromCurrency -> selectFromCurrency(action.country)
            is CurrencyConverterAction.SelectToCurrency -> selectToCurrency(action.country)
            CurrencyConverterAction.SwapCurrencies -> swapCurrencies()
            CurrencyConverterAction.Refresh -> refresh()
        }
    }
}
