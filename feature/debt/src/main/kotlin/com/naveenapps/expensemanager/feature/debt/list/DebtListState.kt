package com.naveenapps.expensemanager.feature.debt.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.debt.DebtUiModel

@Stable
data class DebtListState(
    val isLoading: Boolean,
    val debts: List<DebtUiModel>,
)
