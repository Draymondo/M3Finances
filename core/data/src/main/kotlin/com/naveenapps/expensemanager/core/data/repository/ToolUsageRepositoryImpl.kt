package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.datastore.ToolUsageDataStore
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.repository.ToolUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ToolUsageRepositoryImpl(
    private val toolUsageDataStore: ToolUsageDataStore
) : ToolUsageRepository {

    override fun getToolUsage(): Flow<Map<ToolType, Int>> {
        return toolUsageDataStore.getAllUsage().map { map ->
            map.mapNotNull { (key, count) ->
                ToolType.fromKey(key)?.let { it to count }
            }.toMap()
        }
    }

    override suspend fun incrementUsage(toolType: ToolType) {
        toolUsageDataStore.incrementUsage(toolType.key)
    }

    override suspend fun setToolUsage(usage: Map<ToolType, Int>) {
        toolUsageDataStore.setAllUsage(usage.mapKeys { it.key.key })
    }
}
