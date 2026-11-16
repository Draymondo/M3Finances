package com.naveenapps.expensemanager.feature.installmentplan.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import java.util.Date

sealed class InstallmentPlanCreateAction {

    data object ClosePage : InstallmentPlanCreateAction()

    data object Save : InstallmentPlanCreateAction()

    data object Delete : InstallmentPlanCreateAction()

    data object ShowDeleteDialog : InstallmentPlanCreateAction()

    data object DismissDeleteDialog : InstallmentPlanCreateAction()

    data object ShowDeadlineSelection : InstallmentPlanCreateAction()

    data object DismissDeadlineSelection : InstallmentPlanCreateAction()

    data class SelectDeadline(val date: Date) : InstallmentPlanCreateAction()

    data object ClearDeadline : InstallmentPlanCreateAction()

    data object ToggleCompleted : InstallmentPlanCreateAction()

    data object ShowPaymentSheet : InstallmentPlanCreateAction()

    data object DismissPaymentSheet : InstallmentPlanCreateAction()

    data object TogglePaymentDirection : InstallmentPlanCreateAction()

    data object ShowPaymentAccountSelection : InstallmentPlanCreateAction()

    data object DismissPaymentAccountSelection : InstallmentPlanCreateAction()

    data class SelectPaymentAccount(val account: AccountUiModel) : InstallmentPlanCreateAction()

    data object ConfirmPayment : InstallmentPlanCreateAction()
}
