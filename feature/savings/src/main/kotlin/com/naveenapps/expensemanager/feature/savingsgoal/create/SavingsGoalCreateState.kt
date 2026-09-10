package com.naveenapps.expensemanager.feature.savingsgoal.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import java.util.Date

@Stable
data class SavingsGoalCreateState(
    /** True once an existing goal has loaded — the initial amount and the funding account are
     * fixed at creation (already baked into a recorded transfer, or absent) and shown as
     * read-only; only name, target amount, target date, notes, and the achieved flag stay
     * editable. */
    val isEditing: Boolean,
    val name: TextFieldValue<String>,
    val notes: TextFieldValue<String>,
    val targetAmount: TextFieldValue<String>,
    /** Starting contribution — only used and shown while creating; 0 is fine (start the goal
     * empty, add contributions later). */
    val initialAmount: TextFieldValue<String>,
    val targetDate: Date?,
    val isAchieved: Boolean,
    val savingsStrategy: com.naveenapps.expensemanager.core.model.SavingsStrategy,
    val targetPercentage: TextFieldValue<String>,
    val currency: Currency,
    /** Funding account for the initial contribution — only used/shown while creating, and only
     * required if [initialAmount] is greater than 0. */
    val selectedAccount: AccountUiModel,
    val accounts: List<AccountUiModel>,
    /** The goal account's current balance, formatted — only populated in edit mode. */
    val savedAmount: Amount?,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showAccountSelection: Boolean,
    val showTargetDateSelection: Boolean,
    /** Contribution bottom sheet — only relevant in edit mode. */
    val showContributionSheet: Boolean = false,
    val contributionAmount: TextFieldValue<String> = TextFieldValue(value = "", valueError = false, onValueChange = {}),
    val contributionAccount: AccountUiModel? = null,
    val showContributionAccountSelection: Boolean = false,
    /** False = money going into the goal (contribution); true = money coming back out
     * (withdrawal). */
    val isWithdrawal: Boolean = false,
)
