package com.naveenapps.expensemanager.feature.recurring.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.TransactionType
import java.util.Date

enum class RecurringAccountSelection {
    FROM_ACCOUNT,
    TO_ACCOUNT,
}

enum class RecurringDateSelection {
    START_DATE,
    END_DATE,
}

sealed class RecurringTransactionCreateAction {

    data object ClosePage : RecurringTransactionCreateAction()

    data object Save : RecurringTransactionCreateAction()

    data object Delete : RecurringTransactionCreateAction()

    data object ShowDeleteDialog : RecurringTransactionCreateAction()

    data object DismissDeleteDialog : RecurringTransactionCreateAction()

    data class ChangeTransactionType(val type: TransactionType) : RecurringTransactionCreateAction()

    data class ChangeFrequency(val frequency: RecurrenceFrequency) : RecurringTransactionCreateAction()

    data class ChangeInterval(val interval: Int) : RecurringTransactionCreateAction()

    data object OpenCategoryCreate : RecurringTransactionCreateAction()

    data object OpenAccountCreate : RecurringTransactionCreateAction()

    data object ShowCategorySelection : RecurringTransactionCreateAction()

    data object DismissCategorySelection : RecurringTransactionCreateAction()

    data class SelectCategory(val category: Category) : RecurringTransactionCreateAction()

    data class ShowAccountSelection(val type: RecurringAccountSelection) : RecurringTransactionCreateAction()

    data object DismissAccountSelection : RecurringTransactionCreateAction()

    data class SelectAccount(val account: AccountUiModel) : RecurringTransactionCreateAction()

    data class ShowDateSelection(val type: RecurringDateSelection) : RecurringTransactionCreateAction()

    data object DismissDateSelection : RecurringTransactionCreateAction()

    data class SelectDate(val date: Date) : RecurringTransactionCreateAction()

    data object ClearEndDate : RecurringTransactionCreateAction()

    data object ToggleActive : RecurringTransactionCreateAction()
}
