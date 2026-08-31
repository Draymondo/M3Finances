package com.naveenapps.expensemanager.core.domain.usecase.analysis

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull

class GenerateInsightsUseCase(
    private val geminiRepository: GeminiRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(dataJson: String): Resource<String> {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull()
        if (apiKey.isNullOrBlank()) {
            return Resource.Error(Exception("Clé API Gemini non configurée"))
        }
        return geminiRepository.generateFinancialInsights(dataJson, apiKey)
    }
}
