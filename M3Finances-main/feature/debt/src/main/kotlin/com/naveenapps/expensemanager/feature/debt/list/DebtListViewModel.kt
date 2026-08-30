package com.naveenapps.expensemanager.feature.debt.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.debt.GetDebtsUseCase
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class DebtListViewModel(
    getDebtsUseCase: GetDebtsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DebtListState(
            isLoading = true,
            debts = emptyList(),
        )
    )
    val state = _state.asStateFlow()

    init {
        getDebtsUseCase.invoke().onEach { debts ->
            _state.update {
                it.copy(isLoading = false, debts = debts)
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen(debtId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.DebtCreate(debtId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: DebtListAction) {
        when (action) {
            DebtListAction.ClosePage -> closePage()
            DebtListAction.OpenDebtCreate -> openCreateScreen()
            is DebtListAction.OpenDebtDetail -> openCreateScreen(action.debtId)
        }
    }
}
