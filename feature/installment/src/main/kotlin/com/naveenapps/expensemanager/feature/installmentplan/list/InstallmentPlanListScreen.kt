package com.naveenapps.expensemanager.feature.installmentplan.list

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
import androidx.compose.material.icons.outlined.Payments
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
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.InstallmentPlanUiModel
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.feature.installmentplan.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun InstallmentPlanListScreen(
    viewModel: InstallmentPlanListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    InstallmentPlanListScaffoldView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun InstallmentPlanListScaffoldView(
    state: InstallmentPlanListState,
    onAction: (InstallmentPlanListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(InstallmentPlanListAction.ClosePage) },
                title = stringResource(id = R.string.installment_plans),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction.invoke(InstallmentPlanListAction.OpenPlanCreate) },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        InstallmentPlanListContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun InstallmentPlanListContent(
    state: InstallmentPlanListState,
    onAction: (InstallmentPlanListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.plans.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_installment_plans_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.plans,
                    key = { it.plan.id },
                ) { model ->
                    AppCardView(
                        modifier = ItemSpecModifier,
                        onClick = {
                            onAction.invoke(InstallmentPlanListAction.OpenPlanDetail(model.plan.id))
                        },
                    ) {
                        InstallmentPlanRow(model = model)
                    }
                }
            }
        }
    }
}

private fun daysBetween(from: Date, to: Date): Long =
    TimeUnit.MILLISECONDS.toDays(abs(to.time - from.time))

@Composable
private fun InstallmentPlanRow(model: InstallmentPlanUiModel) {
    val plan = model.plan
    val completed = plan.isCompleted || model.progress >= 1f
    val now = Date()
    val isOverdue = !completed && plan.deadline?.before(now) == true
    val barColor = colorResource(
        id = when {
            completed -> com.naveenapps.expensemanager.core.common.R.color.green_500
            isOverdue -> com.naveenapps.expensemanager.core.common.R.color.red_500
            else -> com.naveenapps.expensemanager.core.common.R.color.orange_500
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
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = barColor,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                )
                val subtitle = plan.deadline?.let { deadline ->
                    when {
                        completed -> stringResource(
                            id = R.string.deadline_on,
                            deadline.toCompleteDateWithDate(),
                        )
                        deadline.before(now) -> stringResource(
                            id = R.string.overdue_by,
                            daysBetween(deadline, now),
                        )
                        else -> stringResource(
                            id = R.string.days_left,
                            daysBetween(now, deadline),
                        )
                    }
                } ?: stringResource(id = R.string.no_deadline)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = model.paidAmount.amountString.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor,
                )
                Text(
                    text = stringResource(
                        id = R.string.of_target,
                        model.targetAmount.amountString.orEmpty(),
                    ),
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
private fun InstallmentPlanListScreenPreview() {
    NaveenAppsPreviewTheme {
        InstallmentPlanListScaffoldView(
            state = InstallmentPlanListState(
                isLoading = false,
                plans = listOf(
                    InstallmentPlanUiModel(
                        plan = InstallmentPlan(
                            id = "1",
                            accountId = "acc1",
                            name = "Téléphone",
                            targetAmount = 300000.0,
                            deadline = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 90),
                            notes = "",
                            isCompleted = false,
                            createdOn = Date(),
                            updatedOn = Date(),
                            account = Account(
                                id = "acc1",
                                name = "Téléphone",
                                type = AccountType.INSTALLMENT,
                                storedIcon = StoredIcon("", ""),
                                createdOn = Date(),
                                updatedOn = Date(),
                                amount = 120000.0,
                            ),
                        ),
                        paidAmount = Amount(120000.0, "120 000 FCFA"),
                        targetAmount = Amount(300000.0, "300 000 FCFA"),
                        remainingAmount = Amount(180000.0, "180 000 FCFA"),
                        progress = 0.4f,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
