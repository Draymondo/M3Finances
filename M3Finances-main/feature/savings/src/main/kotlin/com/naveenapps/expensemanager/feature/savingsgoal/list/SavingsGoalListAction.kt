package com.naveenapps.expensemanager.feature.savingsgoal.list

sealed class SavingsGoalListAction {

    data object ClosePage : SavingsGoalListAction()

    data object OpenSavingsGoalCreate : SavingsGoalListAction()

    data class OpenSavingsGoalDetail(val savingsGoalId: String) : SavingsGoalListAction()
}
