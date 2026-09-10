package com.naveenapps.expensemanager.feature.savingsgoal.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import java.util.Date

sealed class SavingsGoalCreateAction {

    data object ClosePage : SavingsGoalCreateAction()

    data object Save : SavingsGoalCreateAction()

    data object Delete : SavingsGoalCreateAction()

    data object ShowDeleteDialog : SavingsGoalCreateAction()

    data object DismissDeleteDialog : SavingsGoalCreateAction()

    data object OpenAccountCreate : SavingsGoalCreateAction()

    data object ShowAccountSelection : SavingsGoalCreateAction()

    data object DismissAccountSelection : SavingsGoalCreateAction()

    data class SelectAccount(val account: AccountUiModel) : SavingsGoalCreateAction()

    data object ShowTargetDateSelection : SavingsGoalCreateAction()

    data object DismissTargetDateSelection : SavingsGoalCreateAction()

    data class SelectTargetDate(val date: Date) : SavingsGoalCreateAction()

    data object ClearTargetDate : SavingsGoalCreateAction()

    data object ToggleAchieved : SavingsGoalCreateAction()
    
    data object ToggleSavingsStrategy : SavingsGoalCreateAction()

    data object ShowContributionSheet : SavingsGoalCreateAction()

    data object DismissContributionSheet : SavingsGoalCreateAction()

    data object ToggleContributionDirection : SavingsGoalCreateAction()

    data object ShowContributionAccountSelection : SavingsGoalCreateAction()

    data object DismissContributionAccountSelection : SavingsGoalCreateAction()

    data class SelectContributionAccount(val account: AccountUiModel) : SavingsGoalCreateAction()

    data object ConfirmContribution : SavingsGoalCreateAction()
}
