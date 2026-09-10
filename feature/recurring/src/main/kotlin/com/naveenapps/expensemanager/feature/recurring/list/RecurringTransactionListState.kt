package com.naveenapps.expensemanager.feature.recurring.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.RecurringTransactionUiModel

@Stable
data class RecurringTransactionListState(
    val isLoading: Boolean,
    val recurringTransactions: List<RecurringTransactionUiModel>,
)
