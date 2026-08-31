package com.naveenapps.expensemanager.feature.transaction.list

data class TransactionListState(
    val transactionListItem: List<TransactionListItem>,
    val selectedTransactions: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)