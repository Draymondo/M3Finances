package com.naveenapps.expensemanager.feature.transaction.search

sealed class TransactionSearchAction {
    data object ClosePage : TransactionSearchAction()
    data class QueryChanged(val query: String) : TransactionSearchAction()
    data class OpenTransaction(val transactionId: String) : TransactionSearchAction()
}
