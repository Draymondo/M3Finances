package com.naveenapps.expensemanager.feature.shoppinglist.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.TextFieldValue

@Stable
data class ShoppingListCreateState(
    val isEditing: Boolean,
    val name: TextFieldValue<String>,
    val categories: List<Category>,
    val selectedCategory: Category,
    val accounts: List<AccountUiModel>,
    val selectedAccount: AccountUiModel,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showCategorySelection: Boolean,
    val showAccountSelection: Boolean,
)
