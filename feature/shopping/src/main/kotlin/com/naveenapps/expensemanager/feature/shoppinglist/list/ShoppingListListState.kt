package com.naveenapps.expensemanager.feature.shoppinglist.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.ShoppingListUiModel

@Stable
data class ShoppingListListState(
    val isLoading: Boolean,
    val shoppingLists: List<ShoppingListUiModel>,
)
