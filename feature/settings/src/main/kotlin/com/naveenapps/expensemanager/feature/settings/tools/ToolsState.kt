package com.naveenapps.expensemanager.feature.settings.tools

import androidx.compose.ui.graphics.vector.ImageVector
import com.naveenapps.expensemanager.core.model.ToolType

/** Section the tool belongs to on the "Outils" grid screen. */
enum class ToolGroup {
    /** Suivi & planification — scheduled, recurring, calendar, shopping lists. */
    TRACKING,

    /** Argent & objectifs — debts, savings, envelopes, installments. */
    MONEY,

    /** Pratique — currency converter, work time. */
    PRACTICE,
}

data class ToolItemUiModel(
    val type: ToolType,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val usageCount: Int = 0,
    val group: ToolGroup,
    val isNew: Boolean = false,
    /** Mots-clés supplémentaires (string-array) cherchés en plus du titre. */
    val searchAliasRes: Int? = null,
)

data class ToolsState(
    val tools: List<ToolItemUiModel> = emptyList(),
)
