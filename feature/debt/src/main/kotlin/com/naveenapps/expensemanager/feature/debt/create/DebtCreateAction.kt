package com.naveenapps.expensemanager.feature.debt.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.DebtDirection
import java.util.Date

sealed class DebtCreateAction {

    data object ClosePage : DebtCreateAction()

    data object Save : DebtCreateAction()

    data object Delete : DebtCreateAction()

    data object ShowDeleteDialog : DebtCreateAction()

    data object DismissDeleteDialog : DebtCreateAction()

    data class ChangeDirection(val direction: DebtDirection) : DebtCreateAction()

    data object ToggleLinkedToAccount : DebtCreateAction()

    data object OpenAccountCreate : DebtCreateAction()

    data object ShowAccountSelection : DebtCreateAction()

    data object DismissAccountSelection : DebtCreateAction()

    data class SelectAccount(val account: AccountUiModel) : DebtCreateAction()

    data object ShowDueDateSelection : DebtCreateAction()

    data object DismissDueDateSelection : DebtCreateAction()

    data class SelectDueDate(val date: Date) : DebtCreateAction()

    data object ClearDueDate : DebtCreateAction()

    data object ToggleSettled : DebtCreateAction()

    data object ShowRepaymentSheet : DebtCreateAction()

    data object DismissRepaymentSheet : DebtCreateAction()

    data object ShowRepaymentAccountSelection : DebtCreateAction()

    data object DismissRepaymentAccountSelection : DebtCreateAction()

    data class SelectRepaymentAccount(val account: AccountUiModel) : DebtCreateAction()

    data object ConfirmRepayment : DebtCreateAction()

    data object ShowAddReminderDialog : DebtCreateAction()

    data object DismissAddReminderDialog : DebtCreateAction()

    data class AddReminder(val date: Date) : DebtCreateAction()

    data class DeleteReminder(val id: String) : DebtCreateAction()
}
