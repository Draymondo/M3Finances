package com.naveenapps.expensemanager.feature.debt.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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

            state.isEmpty -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_debts_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> DebtSections(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun DebtSections(
    state: DebtListState,
    onAction: (DebtListAction) -> Unit,
) {
    // Les cartes dépliées sont un état d'affichage : pas besoin de passer par le ViewModel.
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var showSettled by remember { mutableStateOf(false) }

    val openDebt: (String) -> Unit = { debtId ->
        onAction.invoke(DebtListAction.OpenDebtDetail(debtId))
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.debtors.isNotEmpty()) {
            item(key = "header_lent") {
                SectionHeader(
                    title = stringResource(id = R.string.section_owed_to_me),
                    count = state.debtors.sumOf { it.debts.size },
                    total = state.debtorsTotal?.amountString,
                )
            }
            items(
                items = state.debtors,
                key = { "lent_${it.key}" },
            ) { group ->
                PersonCard(
                    group = group,
                    isLent = true,
                    isSettled = false,
                    expanded = expanded["lent_${group.key}"] == true,
                    onToggle = { expanded["lent_${group.key}"] = expanded["lent_${group.key}"] != true },
                    onOpenDebt = openDebt,
                )
            }
        }

        if (state.creditors.isNotEmpty()) {
            item(key = "header_borrowed") {
                SectionHeader(
                    title = stringResource(id = R.string.section_i_owe),
                    count = state.creditors.sumOf { it.debts.size },
                    total = state.creditorsTotal?.amountString,
                )
            }
            items(
                items = state.creditors,
                key = { "borrowed_${it.key}" },
            ) { group ->
                PersonCard(
                    group = group,
                    isLent = false,
                    isSettled = false,
                    expanded = expanded["borrowed_${group.key}"] == true,
                    onToggle = { expanded["borrowed_${group.key}"] = expanded["borrowed_${group.key}"] != true },
                    onOpenDebt = openDebt,
                )
            }
        }

        if (state.settled.isNotEmpty()) {
            item(key = "header_settled") {
                SettledHeader(
                    count = state.settled.sumOf { it.debts.size },
                    expanded = showSettled,
                    onToggle = { showSettled = !showSettled },
                )
            }
            if (showSettled) {
                items(
                    items = state.settled,
                    key = { "settled_${it.key}" },
                ) { group ->
                    PersonCard(
                        group = group,
                        isLent = group.debts.first().debt.direction.isLent(),
                        isSettled = true,
                        expanded = expanded["settled_${group.key}"] == true,
                        onToggle = {
                            expanded["settled_${group.key}"] = expanded["settled_${group.key}"] != true
                        },
                        onOpenDebt = openDebt,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    total: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = pluralStringResource(id = R.plurals.debt_count, count = count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (!total.isNullOrBlank()) {
            Text(
                text = total,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettledHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.section_settled),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = pluralStringResource(id = R.plurals.debt_count, count = count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Une carte par personne : le total de ce qu'elle nous doit (ou de ce que nous lui devons).
 *
 * Si la personne n'a qu'une seule dette, la carte ouvre directement le détail — un seul geste.
 * Avec plusieurs dettes, elle se déplie pour les montrer une par une.
 */
@Composable
private fun PersonCard(
    group: PersonDebtGroup,
    isLent: Boolean,
    isSettled: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDebt: (String) -> Unit,
) {
    val directionColor = colorResource(
        id = if (isLent) {
            com.naveenapps.expensemanager.core.common.R.color.green_500
        } else {
            com.naveenapps.expensemanager.core.common.R.color.orange_500
        },
    )
    val canExpand = group.debts.size > 1

    AppCardView(
        modifier = ItemSpecModifier,
        onClick = {
            if (canExpand) {
                onToggle()
            } else {
                onOpenDebt(group.debts.first().debt.id)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .alpha(if (isSettled) 0.6f else 1f),
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
                        text = group.personName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isSettled) TextDecoration.LineThrough else null,
                    )
                    Text(
                        text = subtitleFor(group = group, isSettled = isSettled),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!isSettled && group.hasOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = group.total?.amountString.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSettled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        directionColor
                    },
                )

                if (canExpand) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (canExpand && expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                for (model in group.debts) {
                    DebtDetailRow(
                        model = model,
                        color = directionColor,
                        onClick = { onOpenDebt(model.debt.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun subtitleFor(group: PersonDebtGroup, isSettled: Boolean): String {
    val count = pluralStringResource(id = R.plurals.debt_count, count = group.debts.size, group.debts.size)
    if (isSettled) {
        return "$count · ${stringResource(id = R.string.settled)}"
    }
    val state = when {
        group.hasOverdue -> stringResource(id = R.string.overdue)
        group.nextDueDate != null -> stringResource(
            id = R.string.due_on,
            requireNotNull(group.nextDueDate).toCompleteDateWithDate(),
        )

        else -> stringResource(id = R.string.no_due_date)
    }
    return "$count · $state"
}

@Composable
private fun DebtDetailRow(
    model: DebtUiModel,
    color: Color,
    onClick: () -> Unit,
) {
    val debt = model.debt
    val today = remember { Date() }
    val isOverdue = debt.dueDate?.before(today) == true && !debt.isSettled
    val dueLabel = debt.dueDate?.let { stringResource(id = R.string.due_on, it.toCompleteDateWithDate()) }
        ?: stringResource(id = R.string.no_due_date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = debt.notes.ifBlank { dueLabel },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (debt.notes.isNotBlank()) {
                Text(
                    text = dueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = model.remainingAmount.amountString.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Preview
@Composable
private fun DebtListScreenPreview() {
    val debt = Debt(
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
    )
    val model = DebtUiModel(
        debt = debt,
        remainingAmount = Amount(5000.0, "5 000 FCFA"),
    )
    NaveenAppsPreviewTheme {
        DebtListScaffoldView(
            state = DebtListState(
                isLoading = false,
                debtors = listOf(
                    PersonDebtGroup(
                        key = "awa",
                        personName = "Awa",
                        debts = listOf(model),
                        total = Amount(5000.0, "5 000 FCFA"),
                        hasOverdue = false,
                        nextDueDate = Date(),
                    ),
                ),
                debtorsTotal = Amount(5000.0, "5 000 FCFA"),
            ),
            onAction = {},
        )
    }
}
