package com.naveenapps.expensemanager.feature.debt.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.components.LoadingItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.utils.ItemSpecModifier
import com.naveenapps.expensemanager.core.domain.usecase.debt.DebtUiModel
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.DebtDirection
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.isLent
import com.naveenapps.expensemanager.feature.debt.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun DebtListScreen(
    viewModel: DebtListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    DebtListScaffoldView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun DebtListScaffoldView(
    state: DebtListState,
    onAction: (DebtListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(DebtListAction.ClosePage) },
                title = stringResource(id = R.string.debts),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction.invoke(DebtListAction.OpenDebtCreate) },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        DebtListContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun DebtListContent(
    state: DebtListState,
    onAction: (DebtListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.debts.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_debts_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.debts,
                    key = { it.debt.id },
                ) { model ->
                    AppCardView(
                        modifier = ItemSpecModifier,
                        onClick = {
                            onAction.invoke(DebtListAction.OpenDebtDetail(model.debt.id))
                        },
                    ) {
                        DebtRow(model = model)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtRow(model: DebtUiModel) {
    val debt = model.debt
    val isLent = debt.direction.isLent()
    val directionColor = colorResource(
        id = if (isLent) {
            com.naveenapps.expensemanager.core.common.R.color.green_500
        } else {
            com.naveenapps.expensemanager.core.common.R.color.orange_500
        },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(if (debt.isSettled) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(directionColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isLent) {
                    Icons.AutoMirrored.Filled.TrendingUp
                } else {
                    Icons.AutoMirrored.Filled.TrendingDown
                },
                contentDescription = null,
                tint = directionColor,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = debt.personName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textDecoration = if (debt.isSettled) TextDecoration.LineThrough else null,
            )
            val subtitle = buildString {
                append(
                    if (isLent) {
                        stringResource(id = R.string.owes_you)
                    } else {
                        stringResource(id = R.string.you_owe)
                    },
                )
                debt.dueDate?.let {
                    append(" · ")
                    append(stringResource(id = R.string.due_on, it.toCompleteDateWithDate()))
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = model.remainingAmount.amountString.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = directionColor,
        )
    }
}

@Preview
@Composable
private fun DebtListScreenPreview() {
    NaveenAppsPreviewTheme {
        DebtListScaffoldView(
            state = DebtListState(
                isLoading = false,
                debts = listOf(
                    DebtUiModel(
                        debt = Debt(
                            id = "1",
                            accountId = "acc1",
                            personName = "Awa",
                            direction = DebtDirection.LENT,
                            dueDate = Date(),
                            notes = "",
                            isSettled = false,
                            createdOn = Date(),
                            updatedOn = Date(),
                            account = Account(
                                id = "acc1",
                                name = "Awa",
                                type = AccountType.DEBT,
                                storedIcon = StoredIcon("", ""),
                                createdOn = Date(),
                                updatedOn = Date(),
                                amount = 5000.0,
                            ),
                        ),
                        remainingAmount = Amount(5000.0, "$ 5,000.00"),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
