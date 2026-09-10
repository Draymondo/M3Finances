package com.naveenapps.expensemanager.feature.shoppinglist.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.GetShoppingListsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShoppingListListViewModel(
    getShoppingListsUseCase: GetShoppingListsUseCase,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ShoppingListListState(
            isLoading = true,
            shoppingLists = emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.SHOPPING_LISTS)
        }
        getShoppingListsUseCase.invoke().onEach { shoppingLists ->
            _state.update {
                it.copy(isLoading = false, shoppingLists = shoppingLists)
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListCreate(null))
    }

    private fun openDetailScreen(shoppingListId: String) {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListDetail(shoppingListId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: ShoppingListListAction) {
        when (action) {
            ShoppingListListAction.ClosePage -> closePage()
            ShoppingListListAction.OpenShoppingListCreate -> openCreateScreen()
            is ShoppingListListAction.OpenShoppingListDetail -> openDetailScreen(action.shoppingListId)
        }
    }
}
