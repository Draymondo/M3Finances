package com.naveenapps.expensemanager.feature.shoppinglist.detail

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.model.TextFieldValue

/** One pending item paired with its price already formatted for display in [state.currency]. */
@Stable
data class ShoppingListItemUiModel(
    val item: ShoppingListItem,
    val formattedPrice: Amount,
)

@Stable
data class ShoppingListDetailState(
    val isLoading: Boolean,
    val shoppingList: ShoppingList?,
    val items: List<ShoppingListItemUiModel>,
    val currency: Currency,
    val pendingTotal: Amount,
    val showAddItemSheet: Boolean,
    val itemName: TextFieldValue<String>,
    val itemPrice: TextFieldValue<String>,
)
