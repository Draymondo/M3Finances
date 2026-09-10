package com.naveenapps.expensemanager.core.domain.usecase.tools

import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.repository.ToolUsageRepository
import kotlinx.coroutines.flow.Flow

class GetToolUsageUseCase(
    private val toolUsageRepository: ToolUsageRepository
) {
    operator fun invoke(): Flow<Map<ToolType, Int>> {
        return toolUsageRepository.getToolUsage()
    }
}
