package com.naveenapps.expensemanager.feature.installmentplan.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.GetInstallmentPlansUseCase
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InstallmentPlanListViewModel(
    getInstallmentPlansUseCase: GetInstallmentPlansUseCase,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        InstallmentPlanListState(
            isLoading = true,
            plans = emptyList(),
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.INSTALLMENT_PLANS)
        }
        getInstallmentPlansUseCase.invoke().onEach { plans ->
            _state.update {
                // Active plans first, completed ones last — keeping the original
                // most-recently-created-first order within each group.
                it.copy(
                    isLoading = false,
                    plans = plans.sortedBy { model -> model.plan.isCompleted },
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen(planId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.InstallmentPlanCreate(planId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: InstallmentPlanListAction) {
        when (action) {
            InstallmentPlanListAction.ClosePage -> closePage()
            InstallmentPlanListAction.OpenPlanCreate -> openCreateScreen()
            is InstallmentPlanListAction.OpenPlanDetail -> openCreateScreen(action.planId)
        }
    }
}
