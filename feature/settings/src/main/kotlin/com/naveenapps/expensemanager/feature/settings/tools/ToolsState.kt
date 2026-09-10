package com.naveenapps.expensemanager.feature.settings.tools

import androidx.compose.ui.graphics.vector.ImageVector
import com.naveenapps.expensemanager.core.model.ToolType

data class ToolItemUiModel(
    val type: ToolType,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val usageCount: Int = 0,
)

data class ToolsState(
    val tools: List<ToolItemUiModel> = emptyList(),
)
