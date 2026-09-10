package com.naveenapps.expensemanager.feature.shoppinglist.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.AddShoppingListUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.DeleteShoppingListUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.FindShoppingListByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.UpdateShoppingListUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class ShoppingListCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findShoppingListByIdUseCase: FindShoppingListByIdUseCase,
    private val addShoppingListUseCase: AddShoppingListUseCase,
    private val updateShoppingListUseCase: UpdateShoppingListUseCase,
    private val deleteShoppingListUseCase: DeleteShoppingListUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val accountsAndCategoriesReady = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        ShoppingListCreateState(
            isEditing = false,
            name = TextFieldValue(value = "", valueError = false, onValueChange = this::setName),
            categories = emptyList(),
            selectedCategory = defaultCategory,
            accounts = emptyList(),
            selectedAccount = defaultAccount,
            showDeleteButton = false,
            showDeleteDialog = false,
            showCategorySelection = false,
            showAccountSelection = false,
        ),
    )
    val state = _state.asStateFlow()

    private var editingShoppingList: ShoppingList? = null

    init {
        observeAccountsAndCategories(getCurrencyUseCase, getAllAccountsUseCase, getAllCategoryUseCase)
        loadShoppingListWhenReady(savedStateHandle)
    }

    private fun observeAccountsAndCategories(
        getCurrencyUseCase: GetCurrencyUseCase,
        getAllAccountsUseCase: GetAllAccountsUseCase,
        getAllCategoryUseCase: GetAllCategoryUseCase,
    ) {
        combine(
            getCurrencyUseCase.invoke(),
            getAllAccountsUseCase.invoke(),
            getAllCategoryUseCase.invoke(),
        ) { currency, accounts, categories ->
            val mappedAccounts = accounts.map { account ->
                account.toAccountUiModel(
                    getFormattedAmountUseCase.invoke(account.amount, currency),
                    if (account.type == AccountType.CREDIT) {
                        getFormattedAmountUseCase.invoke(account.getAvailableCreditLimit(), currency)
                    } else {
                        null
                    },
                )
            }
            val expenseCategories = categories.filter { it.type.isExpense() }
            _state.update {
                it.copy(
                    accounts = mappedAccounts,
                    selectedAccount = if (it.isEditing) {
                        it.selectedAccount
                    } else {
                        mappedAccounts.firstOrNull() ?: defaultAccount
                    },
                    categories = expenseCategories,
                    selectedCategory = if (it.isEditing) {
                        it.selectedCategory
                    } else {
                        expenseCategories.firstOrNull() ?: defaultCategory
                    },
                )
            }
            accountsAndCategoriesReady.update { true }
        }.launchIn(viewModelScope)
    }

    private fun loadShoppingListWhenReady(savedStateHandle: SavedStateHandle) {
        val id = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID) ?: return
        viewModelScope.launch {
            accountsAndCategoriesReady.first { it }
            loadEditingShoppingList(id)
        }
    }

    private suspend fun loadEditingShoppingList(id: String) {
        when (val response = findShoppingListByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val shoppingList = response.data
                editingShoppingList = shoppingList
                _state.update {
                    val matchedAccount = it.accounts.find { account ->
                        account.id == shoppingList.accountId
                    } ?: it.selectedAccount
                    it.copy(
                        isEditing = true,
                        name = it.name.copy(value = shoppingList.name),
                        selectedCategory = shoppingList.category,
                        selectedAccount = matchedAccount,
                        showDeleteButton = true,
                    )
                }
            }
        }
    }

    private fun save() {
        val currentState = _state.value

        if (currentState.name.value.isBlank()) {
            _state.update { it.copy(name = it.name.copy(valueError = true)) }
            return
        }

        if (currentState.isEditing) {
            val shoppingList = editingShoppingList ?: return
            viewModelScope.launch {
                val updated = shoppingList.copy(
                    name = currentState.name.value,
                    categoryId = currentState.selectedCategory.id,
                    accountId = currentState.selectedAccount.id,
                    updatedOn = Date(),
                )
                if (updateShoppingListUseCase.invoke(updated) is Resource.Success) {
                    closePage()
                }
            }
            return
        }

        viewModelScope.launch {
            val now = Date()
            val newShoppingList = ShoppingList(
                id = UUID.randomUUID().toString(),
                name = currentState.name.value,
                categoryId = currentState.selectedCategory.id,
                accountId = currentState.selectedAccount.id,
                createdOn = now,
                updatedOn = now,
            )
            if (addShoppingListUseCase.invoke(newShoppingList) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun delete() {
        val shoppingList = editingShoppingList ?: return
        viewModelScope.launch {
            if (deleteShoppingListUseCase.invoke(shoppingList) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun setName(name: String) {
        _state.update {
            it.copy(name = it.name.copy(value = name, valueError = name.isBlank()))
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun openAccountCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountCreate(null))
    }

    private fun openCategoryCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.CategoryCreate(null))
    }

    fun processAction(action: ShoppingListCreateAction) {
        when (action) {
            ShoppingListCreateAction.ClosePage -> closePage()
            ShoppingListCreateAction.Save -> save()
            ShoppingListCreateAction.Delete -> delete()

            ShoppingListCreateAction.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            ShoppingListCreateAction.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }

            ShoppingListCreateAction.OpenCategoryCreate -> openCategoryCreate()
            ShoppingListCreateAction.ShowCategorySelection -> _state.update {
                it.copy(showCategorySelection = true)
            }
            ShoppingListCreateAction.DismissCategorySelection -> _state.update {
                it.copy(showCategorySelection = false)
            }
            is ShoppingListCreateAction.SelectCategory -> _state.update {
                it.copy(selectedCategory = action.category, showCategorySelection = false)
            }

            ShoppingListCreateAction.OpenAccountCreate -> openAccountCreate()
            ShoppingListCreateAction.ShowAccountSelection -> _state.update {
                it.copy(showAccountSelection = true)
            }
            ShoppingListCreateAction.DismissAccountSelection -> _state.update {
                it.copy(showAccountSelection = false)
            }
            is ShoppingListCreateAction.SelectAccount -> _state.update {
                it.copy(selectedAccount = action.account, showAccountSelection = false)
            }
        }
    }

    companion object {
        private val defaultCategory = Category(
            id = "1",
            name = "Shopping",
            type = CategoryType.EXPENSE,
            storedIcon = StoredIcon(name = "ic_calendar", backgroundColor = "#000000"),
            createdOn = Date(),
            updatedOn = Date(),
        )

        private val defaultAccount = AccountUiModel(
            id = "1",
            name = "",
            storedIcon = StoredIcon(name = "", backgroundColor = "#000000"),
            amount = Amount(0.0, "$ 0.00"),
            amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
        )
    }
}
