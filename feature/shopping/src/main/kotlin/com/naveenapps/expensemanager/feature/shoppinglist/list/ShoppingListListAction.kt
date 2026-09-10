package com.naveenapps.expensemanager.feature.shoppinglist.list

sealed class ShoppingListListAction {

    data object ClosePage : ShoppingListListAction()

    data object OpenShoppingListCreate : ShoppingListListAction()

    data class OpenShoppingListDetail(val shoppingListId: String) : ShoppingListListAction()
}
