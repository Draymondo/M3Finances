package com.naveenapps.expensemanager.feature.envelope.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue

@Stable
data class EnvelopeCreateState(
    val isEditing: Boolean,
    val name: TextFieldValue<String>,
    val amount: TextFieldValue<String>,
    val categories: List<Category>,
    val selectedCategory: Category?,
    /** Only surfaces once a save was attempted without a category picked — mirrors the
     * `valueError` flag pattern used by [TextFieldValue] fields, but a category has no text
     * field of its own to attach one to. */
    val categoryError: Boolean,
    val currency: Currency,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showCategorySelection: Boolean,
    /** Set from a failed save — duplicate category/period, or the budget exclusivity rule.
     * Cleared as soon as the person changes anything. */
    val errorMessage: String? = null,
)
