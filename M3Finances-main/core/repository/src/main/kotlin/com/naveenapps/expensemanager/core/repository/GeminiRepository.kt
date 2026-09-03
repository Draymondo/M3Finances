package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.ReceiptData
import com.naveenapps.expensemanager.core.model.Resource

interface GeminiRepository {
    suspend fun scanReceipt(imageBytes: ByteArray, apiKey: String): Resource<ReceiptData>
    
    suspend fun parseWaveNotification(
        notificationText: String,
        source: com.naveenapps.expensemanager.core.model.TransactionSource,
        apiKey: String,
    ): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction>

    suspend fun suggestCategory(note: String, categories: List<String>, apiKey: String): Resource<String>
    
    suspend fun generateMonthlyReport(transactionsInfo: String, apiKey: String): Resource<String>
}
