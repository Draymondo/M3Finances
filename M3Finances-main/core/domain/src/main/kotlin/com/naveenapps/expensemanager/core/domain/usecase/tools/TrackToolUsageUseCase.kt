package com.naveenapps.expensemanager.core.domain.usecase.tools

import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.repository.ToolUsageRepository

class TrackToolUsageUseCase(
    private val toolUsageRepository: ToolUsageRepository
) {
    suspend operator fun invoke(toolType: ToolType) {
        toolUsageRepository.incrementUsage(toolType)
    }
}
