package com.naveenapps.expensemanager.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.feature.calendar.components.CalendarMonthGrid
import com.naveenapps.expensemanager.feature.calendar.components.CalendarWeekRow
import com.naveenapps.expensemanager.feature.calendar.components.CalendarYearGrid
import com.naveenapps.expensemanager.feature.transaction.list.TransactionItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    CalendarContent(
        state = state,
        onAction = { action ->
            if (action is CalendarAction.ClosePage) {
                onBack()
            }
            viewModel.processAction(action)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
) {
    val greenColor = colorResource(com.naveenapps.expensemanager.core.common.R.color.green_500)
    val redColor = colorResource(com.naveenapps.expensemanager.core.common.R.color.red_500)

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                title = stringResource(id = R.string.calendar_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction(CalendarAction.ClosePage) },
                actions = {
                    IconButton(onClick = { onAction(CalendarAction.GoToToday) }) {
                        Icon(
                            imageVector = Icons.Outlined.Today,
                            contentDescription = stringResource(id = R.string.calendar_today),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(CalendarAction.AddTransaction) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.calendar_add_transaction),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Sélecteur de mode : Jour | Semaine | Mois | Année
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    val modes = listOf(
                        CalendarViewMode.DAY to stringResource(R.string.calendar_view_day),
                        CalendarViewMode.WEEK to stringResource(R.string.calendar_view_week),
                        CalendarViewMode.MONTH to stringResource(R.string.calendar_view_month),
                        CalendarViewMode.YEAR to stringResource(R.string.calendar_view_year),
                    )
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            onClick = { onAction(CalendarAction.ChangeViewMode(mode)) },
                            selected = state.viewMode == mode,
                            icon = {},
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            // Bandeau de navigation temporelle : < [Titre de période] >
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onAction(CalendarAction.PreviousPeriod) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = state.periodTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { onAction(CalendarAction.NextPeriod) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                }
            }

            // Barre de synthèse (KPIs) : Revenus | Dépenses | Net
            item {
                AppCardView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Revenus
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.calendar_income),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalIncome.amountString.orEmpty().ifBlank { "0.00" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = greenColor,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Dépenses
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.calendar_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalExpense.amountString.orEmpty().ifBlank { "0.00" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = redColor,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Net
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.calendar_net),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.netAmount.amountString.orEmpty().ifBlank { "0.00" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Corps dynamique selon le mode actif
            when (state.viewMode) {
                CalendarViewMode.DAY -> {
                    // En vue Jour, affichage direct des transactions
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.selectedDayFormatted,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val txCount = state.selectedDayTransactions.size
                            val countText = if (txCount > 1) {
                                stringResource(R.string.calendar_transactions_count_plural, txCount)
                            } else {
                                stringResource(R.string.calendar_transactions_count, txCount)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.selectedDayTransactions.isNotEmpty()) {
                        items(state.selectedDayTransactions, key = { it.id }) { transaction ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                                TransactionItem(
                                    categoryName = transaction.categoryName,
                                    categoryColor = transaction.categoryIcon.backgroundColor,
                                    categoryIcon = transaction.categoryIcon.name,
                                    amount = transaction.amount,
                                    date = transaction.date,
                                    notes = transaction.notes,
                                    transactionType = transaction.transactionType,
                                    fromAccountName = transaction.fromAccountName,
                                    fromAccountIcon = transaction.fromAccountIcon.name,
                                    fromAccountColor = transaction.fromAccountIcon.backgroundColor,
                                    toAccountName = transaction.toAccountName,
                                    toAccountIcon = transaction.toAccountIcon?.name,
                                    toAccountColor = transaction.toAccountIcon?.backgroundColor,
                                    onClick = { onAction(CalendarAction.OpenTransaction(transaction.id)) },
                                )
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.calendar_no_transactions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                CalendarViewMode.WEEK -> {
                    // Ruban des 7 jours de la semaine
                    item {
                        AppCardView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            CalendarWeekRow(
                                days = state.weekDays,
                                selectedDate = state.selectedDate,
                                onDayClick = { onAction(CalendarAction.SelectDay(it)) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }

                    // En-tête du jour sélectionné dans la semaine
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.selectedDayFormatted,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val txCount = state.selectedDayTransactions.size
                            val countText = if (txCount > 1) {
                                stringResource(R.string.calendar_transactions_count_plural, txCount)
                            } else {
                                stringResource(R.string.calendar_transactions_count, txCount)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.selectedDayTransactions.isNotEmpty()) {
                        items(state.selectedDayTransactions, key = { it.id }) { transaction ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                                TransactionItem(
                                    categoryName = transaction.categoryName,
                                    categoryColor = transaction.categoryIcon.backgroundColor,
                                    categoryIcon = transaction.categoryIcon.name,
                                    amount = transaction.amount,
                                    date = transaction.date,
                                    notes = transaction.notes,
                                    transactionType = transaction.transactionType,
                                    fromAccountName = transaction.fromAccountName,
                                    fromAccountIcon = transaction.fromAccountIcon.name,
                                    fromAccountColor = transaction.fromAccountIcon.backgroundColor,
                                    toAccountName = transaction.toAccountName,
                                    toAccountIcon = transaction.toAccountIcon?.name,
                                    toAccountColor = transaction.toAccountIcon?.backgroundColor,
                                    onClick = { onAction(CalendarAction.OpenTransaction(transaction.id)) },
                                )
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.calendar_no_transactions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                CalendarViewMode.MONTH -> {
                    // Grille mensuelle complète
                    item {
                        AppCardView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            CalendarMonthGrid(
                                days = state.calendarDays,
                                selectedDate = state.selectedDate,
                                onDayClick = { onAction(CalendarAction.SelectDay(it)) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }

                    // En-tête du jour sélectionné
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.selectedDayFormatted,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val txCount = state.selectedDayTransactions.size
                            val countText = if (txCount > 1) {
                                stringResource(R.string.calendar_transactions_count_plural, txCount)
                            } else {
                                stringResource(R.string.calendar_transactions_count, txCount)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.selectedDayTransactions.isNotEmpty()) {
                        items(state.selectedDayTransactions, key = { it.id }) { transaction ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                                TransactionItem(
                                    categoryName = transaction.categoryName,
                                    categoryColor = transaction.categoryIcon.backgroundColor,
                                    categoryIcon = transaction.categoryIcon.name,
                                    amount = transaction.amount,
                                    date = transaction.date,
                                    notes = transaction.notes,
                                    transactionType = transaction.transactionType,
                                    fromAccountName = transaction.fromAccountName,
                                    fromAccountIcon = transaction.fromAccountIcon.name,
                                    fromAccountColor = transaction.fromAccountIcon.backgroundColor,
                                    toAccountName = transaction.toAccountName,
                                    toAccountIcon = transaction.toAccountIcon?.name,
                                    toAccountColor = transaction.toAccountIcon?.backgroundColor,
                                    onClick = { onAction(CalendarAction.OpenTransaction(transaction.id)) },
                                )
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.calendar_no_transactions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                CalendarViewMode.YEAR -> {
                    // Grille annuelle des 12 mois
                    item {
                        CalendarYearGrid(
                            months = state.monthsData,
                            onMonthClick = { month -> onAction(CalendarAction.SelectMonth(month)) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }

            // Espace pour le FAB
            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}
