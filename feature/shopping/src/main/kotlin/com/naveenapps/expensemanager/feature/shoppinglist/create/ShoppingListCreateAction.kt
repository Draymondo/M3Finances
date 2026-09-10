package com.naveenapps.expensemanager.feature.shoppinglist.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category

sealed class ShoppingListCreateAction {

    data object ClosePage : ShoppingListCreateAction()

    data object Save : ShoppingListCreateAction()

    data object Delete : ShoppingListCreateAction()

    data object ShowDeleteDialog : ShoppingListCreateAction()

    data object DismissDeleteDialog : ShoppingListCreateAction()

    data object OpenCategoryCreate : ShoppingListCreateAction()

    data object ShowCategorySelection : ShoppingListCreateAction()

    data object DismissCategorySelection : ShoppingListCreateAction()

    data class SelectCategory(val category: Category) : ShoppingListCreateAction()

    data object OpenAccountCreate : ShoppingListCreateAction()

    data object ShowAccountSelection : ShoppingListCreateAction()

    data object DismissAccountSelection : ShoppingListCreateAction()

    data class SelectAccount(val account: AccountUiModel) : ShoppingListCreateAction()
}
