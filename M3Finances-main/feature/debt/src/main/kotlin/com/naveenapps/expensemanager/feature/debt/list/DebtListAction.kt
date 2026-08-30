package com.naveenapps.expensemanager.feature.debt.list

sealed class DebtListAction {

    data object ClosePage : DebtListAction()

    data object OpenDebtCreate : DebtListAction()

    data class OpenDebtDetail(val debtId: String) : DebtListAction()
}
