package com.naveenapps.expensemanager.feature.transaction.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import com.naveenapps.expensemanager.feature.transaction.R
import com.naveenapps.expensemanager.feature.transaction.list.TransactionItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionSearchScreen(
    viewModel: TransactionSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    TransactionSearchScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun TransactionSearchScreenContent(
    state: TransactionSearchState,
    onAction: (TransactionSearchAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                title = stringResource(R.string.search_transactions),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction(TransactionSearchAction.ClosePage) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onAction(TransactionSearchAction.QueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.search_transactions_hint))
                    },
                )
            }

            if (!state.hasSearched) {
                item {
                    SearchMessage(text = stringResource(R.string.search_transactions_prompt))
                }
            } else if (state.results.isEmpty()) {
                item {
                    SearchMessage(text = stringResource(R.string.search_transactions_empty))
                }
            } else {
                items(state.results, key = TransactionUiItem::id) { transaction ->
                    AppCardView {
                        TransactionItem(
                            categoryName = transaction.categoryTitleResId?.let { stringResource(it) }
                                ?: transaction.categoryName,
                            fromAccountName = transaction.fromAccountName,
                            fromAccountIcon = transaction.fromAccountIcon.name,
                            fromAccountColor = transaction.fromAccountIcon.backgroundColor,
                            amount = transaction.amount,
                            date = transaction.date,
                            notes = transaction.notes,
                            categoryColor = transaction.categoryIcon.backgroundColor,
                            categoryIcon = transaction.categoryIcon.name,
                            toAccountName = transaction.toAccountName,
                            toAccountIcon = transaction.toAccountIcon?.name,
                            toAccountColor = transaction.toAccountIcon?.backgroundColor,
                            transactionType = transaction.transactionType,
                            isSplit = transaction.isSplit,
                            splitLabel = stringResource(R.string.split_badge),
                            onClick = {
                                onAction(TransactionSearchAction.OpenTransaction(transaction.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
