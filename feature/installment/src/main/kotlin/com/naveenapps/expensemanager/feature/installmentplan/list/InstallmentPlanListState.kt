package com.naveenapps.expensemanager.feature.installmentplan.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.InstallmentPlanUiModel

@Stable
data class InstallmentPlanListState(
    val isLoading: Boolean,
    val plans: List<InstallmentPlanUiModel>,
)
