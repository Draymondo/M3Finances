package com.naveenapps.expensemanager.core.model

import java.util.Date

data class ReceiptData(
    val amount: Double? = null,
    val merchantName: String? = null,
    val date: Date? = null,
    val suggestedCategory: String? = null,
)

