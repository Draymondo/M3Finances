package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.ReceiptData
import com.naveenapps.expensemanager.core.model.Resource

interface GeminiRepository {
    suspend fun scanReceipt(imageBytes: ByteArray, apiKey: String): Resource<ReceiptData>
}

