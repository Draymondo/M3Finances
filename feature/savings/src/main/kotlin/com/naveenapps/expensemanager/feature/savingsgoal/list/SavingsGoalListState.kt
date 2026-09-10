package com.naveenapps.expensemanager.feature.savingsgoal.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.SavingsGoalUiModel

@Stable
data class SavingsGoalListState(
    val isLoading: Boolean,
    val savingsGoals: List<SavingsGoalUiModel>,
)
