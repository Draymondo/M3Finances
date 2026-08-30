package com.naveenapps.expensemanager.feature.transaction.search

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.TransactionUiItem

@Stable
data class TransactionSearchState(
    val query: String = "",
    val results: List<TransactionUiItem> = emptyList(),
    val hasSearched: Boolean = false,
)
