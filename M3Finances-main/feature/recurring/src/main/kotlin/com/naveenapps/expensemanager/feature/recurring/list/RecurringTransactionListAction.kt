package com.naveenapps.expensemanager.feature.recurring.list

sealed class RecurringTransactionListAction {

    data object ClosePage : RecurringTransactionListAction()

    data object OpenRecurringTransactionCreate : RecurringTransactionListAction()

    data class EditRecurringTransaction(val recurringTransactionId: String) :
        RecurringTransactionListAction()
}
