package com.naveenapps.expensemanager.feature.shoppinglist.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.AddShoppingListItemUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.CheckOffShoppingListItemUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.DeleteShoppingListItemUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.FindShoppingListByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.GetShoppingListItemsUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class ShoppingListDetailViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    getShoppingListItemsUseCase: GetShoppingListItemsUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findShoppingListByIdUseCase: FindShoppingListByIdUseCase,
    private val addShoppingListItemUseCase: AddShoppingListItemUseCase,
    private val checkOffShoppingListItemUseCase: CheckOffShoppingListItemUseCase,
    private val deleteShoppingListItemUseCase: DeleteShoppingListItemUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val shoppingListId: String = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID).orEmpty()

    private val _state = MutableStateFlow(
        ShoppingListDetailState(
            isLoading = true,
            shoppingList = null,
            items = emptyList(),
            currency = getDefaultCurrencyUseCase.invoke(),
            pendingTotal = Amount(0.0, ""),
            showAddItemSheet = false,
            itemName = TextFieldValue(value = "", valueError = false, onValueChange = this::setItemName),
            itemPrice = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setItemPrice,
            ),
        ),
    )
    val state = _state.asStateFlow()

    init {
        loadShoppingList()
        observeItems(getCurrencyUseCase, getShoppingListItemsUseCase)
    }

    private fun loadShoppingList() {
        if (shoppingListId.isBlank()) {
            closePage()
            return
        }
        viewModelScope.launch {
            when (val response = findShoppingListByIdUseCase.invoke(shoppingListId)) {
                is Resource.Error -> closePage()
                is Resource.Success -> _state.update { it.copy(shoppingList = response.data) }
            }
        }
    }

    private fun observeItems(
        getCurrencyUseCase: GetCurrencyUseCase,
        getShoppingListItemsUseCase: GetShoppingListItemsUseCase,
    ) {
        combine(
            getShoppingListItemsUseCase.invoke(shoppingListId),
            getCurrencyUseCase.invoke(),
        ) { items, currency ->
            val formattedItems = items.map { item ->
                ShoppingListItemUiModel(
                    item = item,
                    formattedPrice = getFormattedAmountUseCase.invoke(item.price, currency),
                )
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    items = formattedItems,
                    currency = currency,
                    pendingTotal = getFormattedAmountUseCase.invoke(items.sumOf { item -> item.price }, currency),
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun showAddItemSheet() {
        _state.update {
            it.copy(
                showAddItemSheet = true,
                itemName = it.itemName.copy(value = "", valueError = false),
                itemPrice = it.itemPrice.copy(
                    value = numberFormatRepository.formatForEditing(0.0),
                    valueError = false,
                ),
            )
        }
    }

    private fun addItem() {
        val currentState = _state.value

        if (currentState.itemName.value.isBlank()) {
            _state.update { it.copy(itemName = it.itemName.copy(valueError = true)) }
            return
        }

        val price = numberFormatRepository.parseToDouble(currentState.itemPrice.value)
        if (price == null || price < 0.0) {
            _state.update { it.copy(itemPrice = it.itemPrice.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val response = addShoppingListItemUseCase.invoke(
                ShoppingListItem(
                    id = UUID.randomUUID().toString(),
                    shoppingListId = shoppingListId,
                    name = currentState.itemName.value,
                    price = price,
                    createdOn = Date(),
                ),
            )
            if (response is Resource.Success) {
                _state.update { it.copy(showAddItemSheet = false) }
            }
        }
    }

    private fun checkOffItem(itemId: String) {
        viewModelScope.launch {
            checkOffShoppingListItemUseCase.invoke(itemId)
        }
    }

    private fun deleteItem(itemId: String) {
        viewModelScope.launch {
            deleteShoppingListItemUseCase.invoke(itemId)
        }
    }

    private fun setItemName(name: String) {
        _state.update {
            it.copy(itemName = it.itemName.copy(value = name, valueError = name.isBlank()))
        }
    }

    private fun setItemPrice(price: String) {
        val priceValue = numberFormatRepository.parseToDouble(price)
        _state.update {
            it.copy(
                itemPrice = it.itemPrice.copy(
                    value = price,
                    valueError = price.isBlank() || priceValue == null || priceValue < 0.0,
                ),
            )
        }
    }

    private fun openEdit() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListCreate(shoppingListId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: ShoppingListDetailAction) {
        when (action) {
            ShoppingListDetailAction.ClosePage -> closePage()
            ShoppingListDetailAction.OpenEdit -> openEdit()
            ShoppingListDetailAction.ShowAddItemSheet -> showAddItemSheet()
            ShoppingListDetailAction.DismissAddItemSheet -> _state.update {
                it.copy(showAddItemSheet = false)
            }
            ShoppingListDetailAction.AddItem -> addItem()
            is ShoppingListDetailAction.CheckOffItem -> checkOffItem(action.itemId)
            is ShoppingListDetailAction.DeleteItem -> deleteItem(action.itemId)
        }
    }
}
