package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.ReceiptData
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull

class ScanReceiptUseCase(
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository,
) {
    suspend fun invoke(imageBytes: ByteArray): Resource<ReceiptData> {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull().orEmpty()
        if (apiKey.isBlank()) {
            return Resource.Error(
                Exception("Clé d'API Gemini non configurée. Veuillez l'ajouter dans Paramètres → Intelligence Artificielle.")
            )
        }
        return geminiRepository.scanReceipt(imageBytes, apiKey)
    }
}

