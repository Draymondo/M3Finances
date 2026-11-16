package com.naveenapps.expensemanager.feature.installmentplan.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.InstallmentPaymentUiModel
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import java.util.Date

@Stable
data class InstallmentPlanCreateState(
    /** True once an existing plan has loaded — the paid amount and progress are then shown
     * read-only; only the name, final price, deadline, notes and the completed flag stay
     * editable. */
    val isEditing: Boolean,
    val name: TextFieldValue<String>,
    val notes: TextFieldValue<String>,
    val targetAmount: TextFieldValue<String>,
    val deadline: Date?,
    val isCompleted: Boolean,
    val currency: Currency,
    val accounts: List<AccountUiModel>,
    /** The plan account's current balance, formatted — only populated in edit mode. */
    val paidAmount: Amount?,
    /** What is left to pay, formatted — only populated in edit mode. */
    val remainingAmount: Amount?,
    val progress: Float,
    val payments: List<InstallmentPaymentUiModel>,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showDeadlineSelection: Boolean,
    /** Payment bottom sheet — only relevant in edit mode. */
    val showPaymentSheet: Boolean = false,
    val paymentAmount: TextFieldValue<String> = TextFieldValue(value = "", valueError = false, onValueChange = {}),
    val paymentAccount: AccountUiModel? = null,
    val showPaymentAccountSelection: Boolean = false,
    /** False = money going into the plan (payment); true = money coming back out
     * (refund). */
    val isRefund: Boolean = false,
)
