package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
class ParseWaveNotificationUseCase(
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository,
) {
    suspend fun invoke(notificationText: String): Resource<PendingTransaction> {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull().orEmpty()
        if (apiKey.isBlank()) {
            return Resource.Error(
                Exception("Clé d'API Gemini non configurée.")
            )
        }
        return geminiRepository.parseWaveNotification(notificationText, apiKey)
    }
}
