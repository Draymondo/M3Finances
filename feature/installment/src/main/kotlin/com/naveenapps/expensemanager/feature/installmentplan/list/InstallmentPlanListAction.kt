package com.naveenapps.expensemanager.feature.installmentplan.list

sealed class InstallmentPlanListAction {

    data object ClosePage : InstallmentPlanListAction()

    data object OpenPlanCreate : InstallmentPlanListAction()

    data class OpenPlanDetail(val planId: String) : InstallmentPlanListAction()
}
