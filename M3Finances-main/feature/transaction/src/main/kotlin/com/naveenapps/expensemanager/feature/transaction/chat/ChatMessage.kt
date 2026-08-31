package com.naveenapps.expensemanager.feature.transaction.chat

import android.graphics.Bitmap
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String?,
    val imageBitmap: Bitmap? = null,
    val proposedTransaction: ProposedTransaction? = null,
    val proposedCategory: ProposedCategory? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

data class ProposedTransaction(
    val amount: Double,
    val categoryName: String,
    val note: String,
    val date: String? = null
)

data class ProposedCategory(
    val name: String,
    val type: String // "EXPENSE" or "INCOME"
)

