package com.naveenapps.expensemanager.feature.recurring.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.common.utils.toDateAndMonth
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.components.LoadingItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.utils.ItemSpecModifier
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.RecurringTransactionUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.feature.recurring.R
import com.naveenapps.expensemanager.feature.transaction.list.TransactionItem
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun RecurringTransactionListScreen(
    viewModel: RecurringTransactionListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    RecurringTransactionListScaffoldView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun RecurringTransactionListScaffoldView(
    state: RecurringTransactionListState,
    onAction: (RecurringTransactionListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = {
                    onAction.invoke(RecurringTransactionListAction.ClosePage)
                },
                title = stringResource(id = R.string.recurring_transactions),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onAction.invoke(RecurringTransactionListAction.OpenRecurringTransactionCreate)
                },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        RecurringTransactionListContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun RecurringTransactionListContent(
    state: RecurringTransactionListState,
    onAction: (RecurringTransactionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.recurringTransactions.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_recurring_transaction_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_transaction,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.recurringTransactions,
                    key = { it.recurringTransaction.id },
                ) { model ->
                    AppCardView(modifier = ItemSpecModifier) {
                        RecurringTransactionRow(
                            model = model,
                            onClick = {
                                onAction.invoke(
                                    RecurringTransactionListAction.EditRecurringTransaction(
                                        model.recurringTransaction.id,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringTransactionRow(
    model: RecurringTransactionUiModel,
    onClick: () -> Unit,
) {
    val recurringTransaction = model.recurringTransaction
    val isTransfer = recurringTransaction.type == TransactionType.TRANSFER

    Box(
        modifier = Modifier.alpha(if (recurringTransaction.isActive) 1f else 0.45f),
    ) {
        TransactionItem(
            categoryName = if (isTransfer) {
                stringResource(id = R.string.recurring_transfer)
            } else {
                recurringTransaction.category.titleResId?.let { stringResource(id = it) }
                    ?: recurringTransaction.category.name
            },
            categoryIcon = recurringTransaction.category.storedIcon.name,
            categoryColor = recurringTransaction.category.storedIcon.backgroundColor,
            fromAccountName = recurringTransaction.fromAccount.name,
            fromAccountIcon = recurringTransaction.fromAccount.storedIcon.name,
            fromAccountColor = recurringTransaction.fromAccount.storedIcon.backgroundColor,
            toAccountName = recurringTransaction.toAccount?.name,
            toAccountIcon = recurringTransaction.toAccount?.storedIcon?.name,
            toAccountColor = recurringTransaction.toAccount?.storedIcon?.backgroundColor,
            amount = model.formattedAmount,
            date = recurringScheduleText(
                frequency = recurringTransaction.frequency,
                interval = recurringTransaction.interval,
                nextOccurrenceDate = recurringTransaction.nextOccurrenceDate,
            ),
            notes = recurringTransaction.notes,
            transactionType = recurringTransaction.type,
            onClick = onClick,
        )
    }
}

@Composable
private fun recurringScheduleText(
    frequency: RecurrenceFrequency,
    interval: Int,
    nextOccurrenceDate: Date,
): String {
    val frequencyLabel = if (interval <= 1) {
        stringResource(
            id = when (frequency) {
                RecurrenceFrequency.DAILY -> R.string.recurring_frequency_daily
                RecurrenceFrequency.WEEKLY -> R.string.recurring_frequency_weekly
                RecurrenceFrequency.MONTHLY -> R.string.recurring_frequency_monthly
                RecurrenceFrequency.YEARLY -> R.string.recurring_frequency_yearly
            },
        )
    } else {
        stringResource(
            id = when (frequency) {
                RecurrenceFrequency.DAILY -> R.string.recurring_every_n_days
                RecurrenceFrequency.WEEKLY -> R.string.recurring_every_n_weeks
                RecurrenceFrequency.MONTHLY -> R.string.recurring_every_n_months
                RecurrenceFrequency.YEARLY -> R.string.recurring_every_n_years
            },
            interval,
        )
    }
    return "$frequencyLabel · ${nextOccurrenceDate.toDateAndMonth()}"
}

@Preview
@Composable
private fun RecurringTransactionListScreenPreview() {
    NaveenAppsPreviewTheme {
        RecurringTransactionListScaffoldView(
            state = RecurringTransactionListState(
                isLoading = false,
                recurringTransactions = listOf(
                    RecurringTransactionUiModel(
                        recurringTransaction = RecurringTransaction(
                            id = "1",
                            notes = "Netflix",
                            categoryId = "1",
                            fromAccountId = "1",
                            toAccountId = null,
                            amount = Amount(15.0, "$ 15.00"),
                            type = TransactionType.EXPENSE,
                            frequency = RecurrenceFrequency.MONTHLY,
                            interval = 1,
                            startDate = Date(),
                            nextOccurrenceDate = Date(),
                            createdOn = Date(),
                            updatedOn = Date(),
                        ),
                        formattedAmount = Amount(15.0, "$ 15.00"),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
