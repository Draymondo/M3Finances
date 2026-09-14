package com.naveenapps.expensemanager.feature.settings.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.tools.GetToolUsageUseCase
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.feature.settings.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ToolsViewModel(
    getToolUsageUseCase: GetToolUsageUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(ToolsState())
    val state = _state.asStateFlow()

    init {
        getToolUsageUseCase().onEach { usageMap ->
            val allTools = listOf(
                ToolItemUiModel(
                    type = ToolType.RECURRING_TRANSACTIONS,
                    titleRes = R.string.recurring_transactions_settings,
                    subtitleRes = R.string.recurring_transactions_settings_subtitle,
                    icon = Icons.Outlined.Autorenew,
                    usageCount = usageMap[ToolType.RECURRING_TRANSACTIONS] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.DEBTS,
                    titleRes = R.string.debts_settings,
                    subtitleRes = R.string.debts_settings_subtitle,
                    icon = Icons.Outlined.Handshake,
                    usageCount = usageMap[ToolType.DEBTS] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.SAVINGS_GOALS,
                    titleRes = R.string.savings_goals_settings,
                    subtitleRes = R.string.savings_goals_settings_subtitle,
                    icon = Icons.Outlined.Savings,
                    usageCount = usageMap[ToolType.SAVINGS_GOALS] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.SHOPPING_LISTS,
                    titleRes = R.string.shopping_lists_settings,
                    subtitleRes = R.string.shopping_lists_settings_subtitle,
                    icon = Icons.Outlined.ShoppingCart,
                    usageCount = usageMap[ToolType.SHOPPING_LISTS] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.SCHEDULED_TRANSACTIONS,
                    titleRes = R.string.scheduled_transactions_settings,
                    subtitleRes = R.string.scheduled_transactions_settings_subtitle,
                    icon = Icons.Outlined.DateRange,
                    usageCount = usageMap[ToolType.SCHEDULED_TRANSACTIONS] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.ENVELOPES,
                    titleRes = R.string.envelopes_settings,
                    subtitleRes = R.string.envelopes_settings_subtitle,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    usageCount = usageMap[ToolType.ENVELOPES] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.WORK_TIME,
                    titleRes = R.string.work_time_settings,
                    subtitleRes = R.string.work_time_settings_subtitle,
                    icon = Icons.Outlined.Schedule,
                    usageCount = usageMap[ToolType.WORK_TIME] ?: 0,
                ),
                ToolItemUiModel(
                    type = ToolType.CURRENCY_CONVERTER,
                    titleRes = R.string.currency_converter_settings_tool,
                    subtitleRes = R.string.currency_converter_settings_tool_subtitle,
                    icon = Icons.Outlined.CurrencyExchange,
                    usageCount = usageMap[ToolType.CURRENCY_CONVERTER] ?: 0,
                ),
            )
            val sorted = allTools.sortedByDescending { it.usageCount }
            _state.update { it.copy(tools = sorted) }
        }.launchIn(viewModelScope)
    }

    fun processAction(action: ToolsAction) {
        when (action) {
            ToolsAction.ClosePage -> appComposeNavigator.popBackStack()
            is ToolsAction.OpenTool -> openTool(action.type)
        }
    }

    private fun openTool(type: ToolType) {
        // Navigation only; single source of truth for tracking is each destination ViewModel's init
        when (type) {
            ToolType.RECURRING_TRANSACTIONS -> appComposeNavigator.navigate(ExpenseManagerScreens.RecurringTransactionList)
            ToolType.DEBTS -> appComposeNavigator.navigate(ExpenseManagerScreens.DebtList)
            ToolType.SAVINGS_GOALS -> appComposeNavigator.navigate(ExpenseManagerScreens.SavingsGoalList)
            ToolType.SHOPPING_LISTS -> appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListList)
            ToolType.SCHEDULED_TRANSACTIONS -> appComposeNavigator.navigate(ExpenseManagerScreens.PendingTransactionList)
            ToolType.ENVELOPES -> appComposeNavigator.navigate(ExpenseManagerScreens.EnvelopeList)
            ToolType.WORK_TIME -> appComposeNavigator.navigate(ExpenseManagerScreens.WorkTimeCalculator)
            ToolType.CURRENCY_CONVERTER -> appComposeNavigator.navigate(ExpenseManagerScreens.CurrencyConverter)
        }
    }
}
