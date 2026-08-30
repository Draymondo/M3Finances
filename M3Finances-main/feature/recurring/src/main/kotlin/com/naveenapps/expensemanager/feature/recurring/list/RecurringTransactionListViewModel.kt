package com.naveenapps.expensemanager.feature.recurring.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.GetRecurringTransactionsUseCase
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update


class RecurringTransactionListViewModel(
    getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RecurringTransactionListState(
            isLoading = true,
            recurringTransactions = emptyList(),
        )
    )
    val state = _state.asStateFlow()

    init {
        getRecurringTransactionsUseCase.invoke().onEach { recurringTransactions ->
            _state.update {
                it.copy(
                    isLoading = false,
                    recurringTransactions = recurringTransactions,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen(recurringTransactionId: String? = null) {
        appComposeNavigator.navigate(
            ExpenseManagerScreens.RecurringTransactionCreate(recurringTransactionId),
        )
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: RecurringTransactionListAction) {
        when (action) {
            RecurringTransactionListAction.ClosePage -> closePage()
            is RecurringTransactionListAction.EditRecurringTransaction ->
                openCreateScreen(action.recurringTransactionId)

            RecurringTransactionListAction.OpenRecurringTransactionCreate -> openCreateScreen()
        }
    }
}
