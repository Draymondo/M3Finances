package com.naveenapps.expensemanager.feature.savingsgoal.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
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
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.SavingsGoalUiModel
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.feature.savingsgoal.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun SavingsGoalListScreen(
    viewModel: SavingsGoalListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    SavingsGoalListScaffoldView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun SavingsGoalListScaffoldView(
    state: SavingsGoalListState,
    onAction: (SavingsGoalListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(SavingsGoalListAction.ClosePage) },
                title = stringResource(id = R.string.savings_goals),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction.invoke(SavingsGoalListAction.OpenSavingsGoalCreate) },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        SavingsGoalListContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun SavingsGoalListContent(
    state: SavingsGoalListState,
    onAction: (SavingsGoalListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.savingsGoals.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_savings_goals_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.savingsGoals,
                    key = { it.savingsGoal.id },
                ) { model ->
                    AppCardView(
                        modifier = ItemSpecModifier,
                        onClick = {
                            onAction.invoke(SavingsGoalListAction.OpenSavingsGoalDetail(model.savingsGoal.id))
                        },
                    ) {
                        SavingsGoalRow(model = model)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalRow(model: SavingsGoalUiModel) {
    val savingsGoal = model.savingsGoal
    val achieved = savingsGoal.isAchieved
    val barColor = colorResource(
        id = if (achieved) {
            com.naveenapps.expensemanager.core.common.R.color.green_500
        } else {
            com.naveenapps.expensemanager.core.common.R.color.blue_500
        },
    )

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(barColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    tint = barColor,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = savingsGoal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (achieved) TextDecoration.LineThrough else null,
                )
                val subtitle = savingsGoal.targetDate?.let {
                    stringResource(id = R.string.target_on, it.toCompleteDateWithDate())
                } ?: stringResource(id = R.string.no_target_date)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = model.savedAmount.amountString.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor,
                )
                Text(
                    text = stringResource(id = R.string.of_target, model.targetAmount.amountString.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { model.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.10f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Preview
@Composable
private fun SavingsGoalListScreenPreview() {
    NaveenAppsPreviewTheme {
        SavingsGoalListScaffoldView(
            state = SavingsGoalListState(
                isLoading = false,
                savingsGoals = listOf(
                    SavingsGoalUiModel(
                        savingsGoal = SavingsGoal(
                            id = "1",
                            accountId = "acc1",
                            name = "Vacances",
                            targetAmount = 2000.0,
                            targetDate = Date(),
                            notes = "",
                            isAchieved = false,
                            createdOn = Date(),
                            updatedOn = Date(),
                            account = Account(
                                id = "acc1",
                                name = "Vacances",
                                type = AccountType.SAVINGS_GOAL,
                                storedIcon = StoredIcon("", ""),
                                createdOn = Date(),
                                updatedOn = Date(),
                                amount = 800.0,
                            ),
                        ),
                        savedAmount = Amount(800.0, "$ 800.00"),
                        targetAmount = Amount(2000.0, "$ 2,000.00"),
                        progress = 0.4f,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
