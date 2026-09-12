package com.naveenapps.expensemanager.feature.envelope.list

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.components.LoadingItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.IconAndBackgroundView
import com.naveenapps.expensemanager.core.designsystem.ui.utils.ItemSpecModifier
import com.naveenapps.expensemanager.core.domain.usecase.envelope.EnvelopeUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.feature.envelope.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EnvelopeListScreen(
    viewModel: EnvelopeListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    EnvelopeListScaffoldView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun EnvelopeListScaffoldView(
    state: EnvelopeListState,
    onAction: (EnvelopeListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(EnvelopeListAction.ClosePage) },
                title = stringResource(id = R.string.envelopes),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction.invoke(EnvelopeListAction.OpenEnvelopeCreate) },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        EnvelopeListContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun EnvelopeListContent(
    state: EnvelopeListState,
    onAction: (EnvelopeListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.envelopes.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_envelopes_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.envelopes,
                    key = { it.id },
                ) { model ->
                    AppCardView(
                        modifier = ItemSpecModifier,
                        onClick = {
                            onAction.invoke(EnvelopeListAction.OpenEnvelopeDetail(model.id))
                        },
                    ) {
                        EnvelopeRow(model = model)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvelopeRow(model: EnvelopeUiModel) {
    val barColor = colorResource(
        id = if (model.isExceeded) {
            com.naveenapps.expensemanager.core.common.R.color.red_500
        } else {
            com.naveenapps.expensemanager.core.common.R.color.blue_500
        },
    )

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAndBackgroundView(
                icon = model.categoryIcon?.name.orEmpty(),
                iconBackgroundColor = model.categoryIcon?.backgroundColor ?: "#000000",
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name?.takeIf { it.isNotBlank() } ?: model.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (!model.name.isNullOrBlank()) {
                    Text(
                        text = model.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = model.remainingAmount.amountString.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor,
                )
                Text(
                    text = stringResource(id = R.string.of_envelope, model.amount.amountString.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (model.percent / 100f).coerceIn(0f, 1f) },
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
private fun EnvelopeListScreenPreview() {
    NaveenAppsPreviewTheme {
        EnvelopeListScaffoldView(
            state = EnvelopeListState(
                isLoading = false,
                envelopes = listOf(
                    EnvelopeUiModel(
                        id = "1",
                        categoryId = "1",
                        categoryName = "Nourriture",
                        categoryIcon = StoredIcon("restaurant", "#FF5722"),
                        name = null,
                        selectedMonth = "September 2026",
                        periodType = BudgetPeriod.MONTHLY,
                        amount = Amount(50000.0, "50 000 F"),
                        spentAmount = Amount(32000.0, "32 000 F"),
                        remainingAmount = Amount(18000.0, "18 000 F"),
                        percent = 64f,
                        isExceeded = false,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
