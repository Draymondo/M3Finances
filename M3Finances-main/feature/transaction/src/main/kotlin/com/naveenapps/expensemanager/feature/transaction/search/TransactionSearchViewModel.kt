package com.naveenapps.expensemanager.feature.transaction.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.SearchTransactionsUseCase
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class TransactionSearchViewModel(
    getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    appCoroutineDispatchers: AppCoroutineDispatchers,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(TransactionSearchState())
    val state = _state.asStateFlow()

    init {
        observeSearchResults(getCurrencyUseCase, appCoroutineDispatchers)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearchResults(
        getCurrencyUseCase: GetCurrencyUseCase,
        appCoroutineDispatchers: AppCoroutineDispatchers,
    ) {
        combine(
            getCurrencyUseCase.invoke(),
            query.debounce(300).distinctUntilChanged().flatMapLatest { text ->
                if (text.isBlank()) {
                    flowOf(emptyList())
                } else {
                    searchTransactionsUseCase(text)
                }
            },
        ) { currency, transactions ->
            transactions.orEmpty().map { transaction ->
                transaction.toTransactionUIModel(
                    getFormattedAmountUseCase.invoke(transaction.amount.amount, currency),
                )
            }
        }.onEach { results ->
            _state.update { it.copy(results = results) }
        }.flowOn(appCoroutineDispatchers.computation).launchIn(viewModelScope)
    }

    fun processAction(action: TransactionSearchAction) {
        when (action) {
            TransactionSearchAction.ClosePage -> appComposeNavigator.popBackStack()
            is TransactionSearchAction.QueryChanged -> {
                query.value = action.query
                _state.update { it.copy(query = action.query, hasSearched = action.query.isNotBlank()) }
            }
            is TransactionSearchAction.OpenTransaction -> {
                appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(action.transactionId))
            }
        }
    }
}
