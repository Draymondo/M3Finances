package com.naveenapps.expensemanager.feature.recurring.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.TransactionType
import java.util.Date

@Stable
data class RecurringTransactionCreateState(
    val amount: TextFieldValue<String>,
    val notes: TextFieldValue<String>,
    val transactionType: TransactionType,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val startDate: Date,
    val endDate: Date?,
    val isActive: Boolean,
    val currency: Currency,
    val selectedCategory: Category,
    val selectedFromAccount: AccountUiModel,
    val selectedToAccount: AccountUiModel?,
    val accounts: List<AccountUiModel>,
    val categories: List<Category>,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showCategorySelection: Boolean,
    val showAccountSelection: Boolean,
    val accountSelection: RecurringAccountSelection,
    val showDateSelection: Boolean,
    val dateSelection: RecurringDateSelection,
)
