package com.naveenapps.expensemanager.feature.shoppinglist.detail

sealed class ShoppingListDetailAction {

    data object ClosePage : ShoppingListDetailAction()

    data object OpenEdit : ShoppingListDetailAction()

    data object ShowAddItemSheet : ShoppingListDetailAction()

    data object DismissAddItemSheet : ShoppingListDetailAction()

    data object AddItem : ShoppingListDetailAction()

    data class CheckOffItem(val itemId: String) : ShoppingListDetailAction()

    data class DeleteItem(val itemId: String) : ShoppingListDetailAction()
}
