package com.naveenapps.expensemanager.feature.debt.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.DebtDirection
import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.model.TextFieldValue
import java.util.Date

@Stable
data class DebtCreateState(
    /** True once an existing debt has loaded — direction, initial amount, and the real account
     * are fixed at creation (they're already baked into a recorded transfer) and shown as
     * read-only; only name, due date, notes, and the settled flag stay editable. */
    val isEditing: Boolean,
    val personName: TextFieldValue<String>,
    val notes: TextFieldValue<String>,
    val direction: DebtDirection,
    /** Initial lend/borrow amount — only used and shown while creating. */
    val amount: TextFieldValue<String>,
    /** True when this debt corresponds to money actually moving out of / into one of the
     * user's own accounts (the common "I lent/borrowed cash" case) — the account picker below
     * is only shown then. False for a debt with no such movement (e.g. money owed for work or
     * services rendered) — only used and shown while creating. */
    val linkedToAccount: Boolean,
    val dueDate: Date?,
    val isSettled: Boolean,
    val currency: Currency,
    val selectedAccount: AccountUiModel,
    val accounts: List<AccountUiModel>,
    /** The debt account's current balance, formatted — only populated in edit mode. */
    val remainingAmount: Amount?,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showAccountSelection: Boolean,
    val showDueDateSelection: Boolean,
    /** Repayment bottom sheet — only relevant in edit mode. */
    val showRepaymentSheet: Boolean = false,
    val repaymentAmount: TextFieldValue<String> = TextFieldValue(value = "", valueError = false, onValueChange = {}),
    val repaymentAccount: AccountUiModel? = null,
    val showRepaymentAccountSelection: Boolean = false,
    /** Reminder dates for this debt — only loaded/shown in edit mode, since a new debt has
     * nowhere to attach a reminder row to until it's actually saved (see DebtCreateViewModel). */
    val reminders: List<DebtReminder> = emptyList(),
    val showAddReminderDialog: Boolean = false,
)
