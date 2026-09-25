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
    val source: TransactionSource = TransactionSource.UNKNOWN,
    val confidence: Float? = null,
    val scheduledDate: Date? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val providerReference: String? = null,
    val deduplicationKey: String? = null,
    val channel: TransactionChannel? = null,
    val notificationEventId: String? = null,
)