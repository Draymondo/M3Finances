package com.naveenapps.expensemanager.core.model

import java.util.Date

data class PendingTransaction(
    val id: String,
    val amount: Double,
    val fee: Double?,
    val merchant: String?,
    val date: Date,
    val transactionType: TransactionType,
    val suggestedCategory: String?,
    val rawNotification: String?,
)

