package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull

class SuggestCategoryUseCase(
    private val geminiRepository: GeminiRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(note: String, categories: List<String>): Resource<String> {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull()
        if (apiKey.isNullOrBlank()) {
            return Resource.Error(Exception("Clé API Gemini non configurée"))
        }
        return geminiRepository.suggestCategory(note, categories, apiKey)
    }
}
