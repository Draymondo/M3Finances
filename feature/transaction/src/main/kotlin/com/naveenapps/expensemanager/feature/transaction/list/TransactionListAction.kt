package com.naveenapps.expensemanager.feature.transaction.list

sealed class TransactionListAction {

    data object ClosePage : TransactionListAction()

    data object OpenCreateTransaction : TransactionListAction()

    data object OpenSearch : TransactionListAction()

    data class OpenEdiTransaction(val transactionId: String) : TransactionListAction()

    data class ToggleSelection(val transactionId: String) : TransactionListAction()

    data object ClearSelection : TransactionListAction()

    data object DeleteSelected : TransactionListAction()
}