package com.naveenapps.expensemanager.feature.savingsgoal.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.GetSavingsGoalsUseCase
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class SavingsGoalListViewModel(
    getSavingsGoalsUseCase: GetSavingsGoalsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SavingsGoalListState(
            isLoading = true,
            savingsGoals = emptyList(),
        )
    )
    val state = _state.asStateFlow()

    init {
        getSavingsGoalsUseCase.invoke().onEach { savingsGoals ->
            _state.update {
                it.copy(isLoading = false, savingsGoals = savingsGoals)
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen(savingsGoalId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.SavingsGoalCreate(savingsGoalId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: SavingsGoalListAction) {
        when (action) {
            SavingsGoalListAction.ClosePage -> closePage()
            SavingsGoalListAction.OpenSavingsGoalCreate -> openCreateScreen()
            is SavingsGoalListAction.OpenSavingsGoalDetail -> openCreateScreen(action.savingsGoalId)
        }
    }
}
