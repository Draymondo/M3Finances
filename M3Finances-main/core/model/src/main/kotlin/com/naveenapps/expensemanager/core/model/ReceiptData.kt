package com.naveenapps.expensemanager.core.model

import java.util.Date

data class ReceiptItemData(
    val name: String? = null,
    val amount: Double? = null,
    val suggestedCategory: String? = null,
)

data class ReceiptData(
    val amount: Double? = null,
    val merchantName: String? = null,
    val date: Date? = null,
    val suggestedCategory: String? = null,
    val items: List<ReceiptItemData>? = null,
)

