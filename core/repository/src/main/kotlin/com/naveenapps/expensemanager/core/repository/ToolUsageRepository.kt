package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.ToolType
import kotlinx.coroutines.flow.Flow

interface ToolUsageRepository {
    fun getToolUsage(): Flow<Map<ToolType, Int>>
    suspend fun incrementUsage(toolType: ToolType)
    suspend fun setToolUsage(usage: Map<ToolType, Int>)
}
