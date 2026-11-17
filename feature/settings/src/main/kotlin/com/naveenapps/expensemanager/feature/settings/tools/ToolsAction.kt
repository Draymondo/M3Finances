package com.naveenapps.expensemanager.feature.settings.tools

import com.naveenapps.expensemanager.core.model.ToolType

sealed class ToolsAction {
    data object ClosePage : ToolsAction()
    data class OpenTool(val type: ToolType) : ToolsAction()
    data class UpdateSearchQuery(val query: String) : ToolsAction()
}
