package com.naveenapps.expensemanager.feature.recurring.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.designsystem.components.DeleteDialogItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppDatePickerDialog
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppFilterChip
import com.naveenapps.expensemanager.core.designsystem.ui.components.ClickableTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.feature.account.selection.AccountItem
import com.naveenapps.expensemanager.feature.account.selection.AccountItemDefaults
import com.naveenapps.expensemanager.feature.account.selection.AccountSelectionScreen
import com.naveenapps.expensemanager.feature.category.selection.CategoryItem
import com.naveenapps.expensemanager.feature.category.selection.CategoryItemDefaults
import com.naveenapps.expensemanager.feature.category.selection.CategorySelectionScreen
import com.naveenapps.expensemanager.feature.recurring.R
import com.naveenapps.expensemanager.feature.transaction.create.TransactionTypeSelectionView
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun RecurringTransactionCreateScreen(
    viewModel: RecurringTransactionCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    RecurringTransactionCreateScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun RecurringTransactionCreateScreenContent(
    state: RecurringTransactionCreateState,
    onAction: (RecurringTransactionCreateAction) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = { onAction.invoke(RecurringTransactionCreateAction.Delete) },
            dismiss = { onAction.invoke(RecurringTransactionCreateAction.DismissDeleteDialog) },
        )
    } else if (state.showCategorySelection) {
        CategorySelectionView(state, onAction)
    } else if (state.showAccountSelection) {
        AccountSelectionView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = {
                    onAction.invoke(RecurringTransactionCreateAction.ClosePage)
                },
                title = if (state.showDeleteButton) {
                    stringResource(R.string.edit_recurring_transaction)
                } else {
                    stringResource(R.string.create_recurring_transaction)
                },
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(
                            onClick = {
                                onAction.invoke(RecurringTransactionCreateAction.ShowDeleteDialog)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(
                                    com.naveenapps.expensemanager.feature.transaction.R.string.delete,
                                ),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(RecurringTransactionCreateAction.Save) },
                icon = {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                },
                text = {
                    Text(
                        text = stringResource(
                            com.naveenapps.expensemanager.feature.transaction.R.string.save,
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        RecurringTransactionCreateBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountSelectionView(
    state: RecurringTransactionCreateState,
    onAction: (RecurringTransactionCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = {
            onAction.invoke(RecurringTransactionCreateAction.DismissAccountSelection)
        },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = when (state.accountSelection) {
                RecurringAccountSelection.FROM_ACCOUNT -> state.selectedFromAccount
                RecurringAccountSelection.TO_ACCOUNT -> state.selectedToAccount
            },
            createNewCallback = {
                onAction.invoke(RecurringTransactionCreateAction.OpenAccountCreate)
            },
            onItemSelection = {
                onAction.invoke(RecurringTransactionCreateAction.SelectAccount(it))
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategorySelectionView(
    state: RecurringTransactionCreateState,
    onAction: (RecurringTransactionCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = {
            onAction.invoke(RecurringTransactionCreateAction.DismissCategorySelection)
        },
    ) {
        CategorySelectionScreen(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            createNewCallback = {
                onAction.invoke(RecurringTransactionCreateAction.OpenCategoryCreate)
            },
            onItemSelection = {
                onAction.invoke(RecurringTransactionCreateAction.SelectCategory(it))
            },
        )
    }
}

@Composable
private fun RecurringTransactionCreateBody(
    state: RecurringTransactionCreateState,
    onAction: (RecurringTransactionCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    if (state.showDateSelection) {
        AppDatePickerDialog(
            selectedDate = when (state.dateSelection) {
                RecurringDateSelection.START_DATE -> state.startDate
                RecurringDateSelection.END_DATE -> state.endDate ?: state.startDate
            },
            onDateSelected = {
                onAction.invoke(RecurringTransactionCreateAction.SelectDate(it))
            },
            onDismiss = {
                onAction.invoke(RecurringTransactionCreateAction.DismissDateSelection)
            },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsSection(
            title = stringResource(
                com.naveenapps.expensemanager.feature.transaction.R.string.transaction_type,
            ),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            AppCardView {
                Column(modifier = Modifier.padding(16.dp)) {
                    TransactionTypeSelectionView(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTransactionType = state.transactionType,
                        onTransactionTypeChange = {
                            onAction.invoke(
                                RecurringTransactionCreateAction.ChangeTransactionType(it),
                            )
                        },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(
                com.naveenapps.expensemanager.feature.transaction.R.string.details,
            ),
        ) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.amount.value,
                        isError = state.amount.valueError,
                        onValueChange = state.amount.onValueChange,
                        label = com.naveenapps.expensemanager.feature.transaction.R.string.amount,
                        errorMessage = stringResource(id = R.string.recurring_amount_error),
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.notes.value,
                        singleLine = false,
                        maxLines = 3,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(
                                    com.naveenapps.expensemanager.feature.transaction.R.string.optional_details,
                                ),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(
                                    com.naveenapps.expensemanager.feature.transaction.R.string.notes,
                                ),
                            )
                        },
                        onValueChange = { state.notes.onValueChange?.invoke(it) },
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus(force = true) },
                        ),
                    )
                }
            }
        }

        // Category — only for non-transfer
        AnimatedVisibility(
            visible = state.transactionType != TransactionType.TRANSFER,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            SettingsSection(
                title = stringResource(
                    com.naveenapps.expensemanager.feature.transaction.R.string.select_category,
                ),
            ) {
                CategoryItem(
                    name = state.selectedCategory.titleResId?.let { stringResource(it) }
                        ?: state.selectedCategory.name,
                    icon = state.selectedCategory.storedIcon.name,
                    iconBackgroundColor = state.selectedCategory.storedIcon.backgroundColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(RecurringTransactionCreateAction.ShowCategorySelection)
                    },
                    trailingContent = { CategoryItemDefaults.ChevronTrailing() },
                )
            }
        }

        SettingsSection(
            title = stringResource(
                id = if (state.transactionType == TransactionType.TRANSFER) {
                    com.naveenapps.expensemanager.feature.transaction.R.string.from_account
                } else {
                    com.naveenapps.expensemanager.feature.transaction.R.string.select_account
                },
            ),
        ) {
            AccountItem(
                name = state.selectedFromAccount.name,
                icon = state.selectedFromAccount.storedIcon.name,
                iconBackgroundColor = state.selectedFromAccount.storedIcon.backgroundColor,
                amount = state.selectedFromAccount.amount.amountString,
                amountTextColor = state.selectedFromAccount.amountTextColor,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    focusManager.clearFocus(force = true)
                    onAction.invoke(
                        RecurringTransactionCreateAction.ShowAccountSelection(
                            RecurringAccountSelection.FROM_ACCOUNT,
                        ),
                    )
                },
                trailingContent = { AccountItemDefaults.ChevronTrailing() },
            )
        }

        // To Account — only for transfer
        AnimatedVisibility(
            visible = state.transactionType == TransactionType.TRANSFER,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            SettingsSection(
                title = stringResource(
                    com.naveenapps.expensemanager.feature.transaction.R.string.to_account,
                ),
            ) {
                if (state.selectedToAccount != null) {
                    AccountItem(
                        name = state.selectedToAccount.name,
                        icon = state.selectedToAccount.storedIcon.name,
                        iconBackgroundColor = state.selectedToAccount.storedIcon.backgroundColor,
                        amount = state.selectedToAccount.amount.amountString,
                        amountTextColor = state.selectedToAccount.amountTextColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAction.invoke(
                                RecurringTransactionCreateAction.ShowAccountSelection(
                                    RecurringAccountSelection.TO_ACCOUNT,
                                ),
                            )
                        },
                        trailingContent = { AccountItemDefaults.ChevronTrailing() },
                    )
                }
            }
        }

        SettingsSection(title = stringResource(id = R.string.schedule)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecurrenceFrequency.entries.forEach { frequency ->
                            AppFilterChip(
                                filterName = stringResource(id = frequency.labelRes()),
                                isSelected = state.frequency == frequency,
                                onClick = {
                                    onAction.invoke(
                                        RecurringTransactionCreateAction.ChangeFrequency(frequency),
                                    )
                                },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(id = R.string.repeat_every),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        IntervalStepper(
                            interval = state.interval,
                            onIntervalChange = { newInterval ->
                                onAction.invoke(
                                    RecurringTransactionCreateAction.ChangeInterval(newInterval),
                                )
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ClickableTextField(
                            modifier = Modifier.weight(1f),
                            value = state.startDate.toCompleteDateWithDate(),
                            label = R.string.start_date,
                            leadingIcon = Icons.Outlined.EditCalendar,
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onAction.invoke(
                                    RecurringTransactionCreateAction.ShowDateSelection(
                                        RecurringDateSelection.START_DATE,
                                    ),
                                )
                            },
                        )
                        ClickableTextField(
                            modifier = Modifier.weight(1f),
                            value = state.endDate?.toCompleteDateWithDate()
                                ?: stringResource(id = R.string.no_end_date),
                            label = R.string.end_date,
                            leadingIcon = Icons.Outlined.EditCalendar,
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onAction.invoke(
                                    RecurringTransactionCreateAction.ShowDateSelection(
                                        RecurringDateSelection.END_DATE,
                                    ),
                                )
                            },
                        )
                    }

                    if (state.endDate != null) {
                        TextButton(
                            onClick = { onAction.invoke(RecurringTransactionCreateAction.ClearEndDate) },
                        ) {
                            Text(text = stringResource(id = R.string.clear))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.active),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = stringResource(id = R.string.active_recurring_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.isActive,
                            onCheckedChange = {
                                onAction.invoke(RecurringTransactionCreateAction.ToggleActive)
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun IntervalStepper(
    interval: Int,
    onIntervalChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { if (interval > 1) onIntervalChange(interval - 1) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Remove, contentDescription = null)
        }
        Text(
            text = interval.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        FilledTonalIconButton(
            onClick = { onIntervalChange(interval + 1) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }
}

private fun RecurrenceFrequency.labelRes(): Int = when (this) {
    RecurrenceFrequency.DAILY -> R.string.recurring_frequency_daily
    RecurrenceFrequency.WEEKLY -> R.string.recurring_frequency_weekly
    RecurrenceFrequency.MONTHLY -> R.string.recurring_frequency_monthly
    RecurrenceFrequency.YEARLY -> R.string.recurring_frequency_yearly
}

@AppPreviewsLightAndDarkMode
@Composable
private fun RecurringTransactionCreateScreenPreview() {
    val amountField = TextFieldValue(value = "100", valueError = false, onValueChange = {})
    val notesField = TextFieldValue(value = "", valueError = false, onValueChange = {})
    NaveenAppsPreviewTheme {
        RecurringTransactionCreateScreenContent(
            state = RecurringTransactionCreateState(
                amount = amountField,
                notes = notesField,
                transactionType = TransactionType.EXPENSE,
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                startDate = Date(),
                endDate = null,
                isActive = true,
                currency = Currency("$", "USD"),
                selectedCategory = Category(
                    id = "1",
                    name = "Subscriptions",
                    type = CategoryType.EXPENSE,
                    storedIcon = StoredIcon("ic_calendar", "#000000"),
                    createdOn = Date(),
                    updatedOn = Date(),
                ),
                selectedFromAccount = AccountUiModel(
                    id = "1",
                    name = "Wallet",
                    storedIcon = StoredIcon("ic_calendar", "#000000"),
                    amount = Amount(0.0, "$ 0.00"),
                    amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
                ),
                selectedToAccount = null,
                accounts = emptyList(),
                categories = emptyList(),
                showDeleteButton = true,
                showDeleteDialog = false,
                showCategorySelection = false,
                showAccountSelection = false,
                accountSelection = RecurringAccountSelection.FROM_ACCOUNT,
                showDateSelection = false,
                dateSelection = RecurringDateSelection.START_DATE,
            ),
            onAction = {},
        )
    }
}
