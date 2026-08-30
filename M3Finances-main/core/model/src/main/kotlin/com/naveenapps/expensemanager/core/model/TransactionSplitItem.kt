package com.naveenapps.expensemanager.core.model

import androidx.compose.runtime.Stable

@Stable
data class TransactionSplitItem(
    val id: String,
    val transactionId: String,
    val categoryId: String,
    val amount: Amount,
    val notes: String? = null,
    var category: Category? = null,
)
