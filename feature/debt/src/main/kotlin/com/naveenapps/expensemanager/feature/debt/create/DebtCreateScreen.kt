package com.naveenapps.expensemanager.feature.debt.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.naveenapps.expensemanager.core.designsystem.ui.components.StringTextField
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.DebtDirection
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.isLent
import com.naveenapps.expensemanager.feature.account.selection.AccountItem
import com.naveenapps.expensemanager.feature.account.selection.AccountItemDefaults
import com.naveenapps.expensemanager.feature.account.selection.AccountSelectionScreen
import com.naveenapps.expensemanager.feature.debt.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebtCreateScreen(
    viewModel: DebtCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    DebtCreateScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun DebtCreateScreenContent(
    state: DebtCreateState,
    onAction: (DebtCreateAction) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = { onAction.invoke(DebtCreateAction.Delete) },
            dismiss = { onAction.invoke(DebtCreateAction.DismissDeleteDialog) },
        )
    } else if (state.showAccountSelection) {
        AccountSelectionView(state, onAction)
    } else if (state.showRepaymentAccountSelection) {
        RepaymentAccountSelectionView(state, onAction)
    } else if (state.showRepaymentSheet) {
        RepaymentSheetView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(DebtCreateAction.ClosePage) },
                title = if (state.showDeleteButton) {
                    stringResource(R.string.edit_debt)
                } else {
                    stringResource(R.string.create_debt)
                },
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(
                            onClick = { onAction.invoke(DebtCreateAction.ShowDeleteDialog) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_debt),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(DebtCreateAction.Save) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                text = { Text(text = stringResource(R.string.save)) },
            )
        },
    ) { innerPadding ->
        DebtCreateBody(
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
    state: DebtCreateState,
    onAction: (DebtCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(DebtCreateAction.DismissAccountSelection) },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = state.selectedAccount,
            createNewCallback = { onAction.invoke(DebtCreateAction.OpenAccountCreate) },
            onItemSelection = { onAction.invoke(DebtCreateAction.SelectAccount(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RepaymentAccountSelectionView(
    state: DebtCreateState,
    onAction: (DebtCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(DebtCreateAction.DismissRepaymentAccountSelection) },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = state.repaymentAccount,
            createNewCallback = { onAction.invoke(DebtCreateAction.OpenAccountCreate) },
            onItemSelection = { onAction.invoke(DebtCreateAction.SelectRepaymentAccount(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RepaymentSheetView(
    state: DebtCreateState,
    onAction: (DebtCreateAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(DebtCreateAction.DismissRepaymentSheet) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.record_repayment),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            DecimalTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.repaymentAmount.value,
                isError = state.repaymentAmount.valueError,
                onValueChange = state.repaymentAmount.onValueChange,
                label = R.string.repayment_amount,
                errorMessage = stringResource(R.string.amount_error),
            )

            state.repaymentAccount?.let { repaymentAccount ->
                AccountItem(
                    name = repaymentAccount.name,
                    icon = repaymentAccount.storedIcon.name,
                    iconBackgroundColor = repaymentAccount.storedIcon.backgroundColor,
                    amount = repaymentAccount.amount.amountString,
                    amountTextColor = repaymentAccount.amountTextColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(DebtCreateAction.ShowRepaymentAccountSelection)
                    },
                    trailingContent = { AccountItemDefaults.ChevronTrailing() },
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction.invoke(DebtCreateAction.ConfirmRepayment) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun DebtCreateBody(
    state: DebtCreateState,
    onAction: (DebtCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    if (state.showDueDateSelection) {
        AppDatePickerDialog(
            selectedDate = state.dueDate ?: java.util.Date(),
            onDateSelected = { onAction.invoke(DebtCreateAction.SelectDueDate(it)) },
            onDismiss = { onAction.invoke(DebtCreateAction.DismissDueDateSelection) },
        )
    }

    if (state.showAddReminderDialog) {
        AppDatePickerDialog(
            selectedDate = java.util.Date(),
            onDateSelected = { onAction.invoke(DebtCreateAction.AddReminder(it)) },
            onDismiss = { onAction.invoke(DebtCreateAction.DismissAddReminderDialog) },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Direction — fixed once created, since it determines which way the recorded transfer
        // runs; only shown as a chip picker while creating a new debt.
        if (!state.isEditing) {
            SettingsSection(
                title = stringResource(R.string.direction),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                AppCardView {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppFilterChip(
                            filterName = stringResource(R.string.direction_lent),
                            isSelected = state.direction == DebtDirection.LENT,
                            onClick = {
                                onAction.invoke(DebtCreateAction.ChangeDirection(DebtDirection.LENT))
                            },
                        )
                        AppFilterChip(
                            filterName = stringResource(R.string.direction_borrowed),
                            isSelected = state.direction == DebtDirection.BORROWED,
                            onClick = {
                                onAction.invoke(DebtCreateAction.ChangeDirection(DebtDirection.BORROWED))
                            },
                        )
                    }
                }
            }
        } else {
            SettingsSection(
                title = stringResource(R.string.remaining_amount),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                AppCardView {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (state.direction.isLent()) {
                                stringResource(R.string.owes_you)
                            } else {
                                stringResource(R.string.you_owe)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = state.remainingAmount?.amountString.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (!state.isSettled && state.accounts.isNotEmpty()) {
                        TextButton(
                            modifier = Modifier.padding(bottom = 8.dp),
                            onClick = { onAction.invoke(DebtCreateAction.ShowRepaymentSheet) },
                        ) {
                            Text(text = stringResource(R.string.record_repayment))
                        }
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.details)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StringTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.personName.value,
                        isError = state.personName.valueError,
                        onValueChange = state.personName.onValueChange,
                        label = R.string.person_name,
                        errorMessage = stringResource(R.string.person_name_error),
                    )

                    if (!state.isEditing) {
                        DecimalTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.amount.value,
                            isError = state.amount.valueError,
                            onValueChange = state.amount.onValueChange,
                            label = R.string.initial_amount,
                            errorMessage = stringResource(R.string.amount_error),
                        )
                    }

                    StringTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.notes.value,
                        isError = false,
                        onValueChange = state.notes.onValueChange,
                        label = R.string.notes,
                        minLines = 2,
                    )
                }
            }
        }

        // Account — only shown while creating; when linked, the real account is fixed
        // afterward, baked into the already-recorded transfer.
        AnimatedVisibility(
            visible = !state.isEditing,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            SettingsSection(title = stringResource(R.string.account)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCardView {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.linked_to_account),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stringResource(R.string.linked_to_account_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.linkedToAccount,
                                onCheckedChange = {
                                    onAction.invoke(DebtCreateAction.ToggleLinkedToAccount)
                                },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.linkedToAccount,
                        enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                    ) {
                        AccountItem(
                            name = state.selectedAccount.name,
                            icon = state.selectedAccount.storedIcon.name,
                            iconBackgroundColor = state.selectedAccount.storedIcon.backgroundColor,
                            amount = state.selectedAccount.amount.amountString,
                            amountTextColor = state.selectedAccount.amountTextColor,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onAction.invoke(DebtCreateAction.ShowAccountSelection)
                            },
                            trailingContent = { AccountItemDefaults.ChevronTrailing() },
                        )
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.due_date)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClickableTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.dueDate?.toCompleteDateWithDate()
                            ?: stringResource(R.string.no_due_date),
                        label = R.string.due_date,
                        leadingIcon = Icons.Outlined.EditCalendar,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAction.invoke(DebtCreateAction.ShowDueDateSelection)
                        },
                    )
                    if (state.dueDate != null) {
                        TextButton(onClick = { onAction.invoke(DebtCreateAction.ClearDueDate) }) {
                            Text(text = stringResource(R.string.clear))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.isEditing,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            SettingsSection(title = stringResource(R.string.reminders)) {
                AppCardView {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.reminders.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_reminders),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.reminders.forEach { reminder ->
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(text = reminder.reminderDate.toCompleteDateWithDate())
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.delete_reminder),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        onAction.invoke(
                                                            DebtCreateAction.DeleteReminder(reminder.id),
                                                        )
                                                    },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { onAction.invoke(DebtCreateAction.ShowAddReminderDialog) },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.add_reminder))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.isEditing,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            AppCardView {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settled),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.settled_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isSettled,
                        onCheckedChange = { onAction.invoke(DebtCreateAction.ToggleSettled) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun DebtCreateScreenPreview() {
    NaveenAppsPreviewTheme {
        DebtCreateScreenContent(
            state = DebtCreateState(
                isEditing = false,
                personName = TextFieldValue(value = "Awa", valueError = false, onValueChange = {}),
                notes = TextFieldValue(value = "", valueError = false, onValueChange = {}),
                direction = DebtDirection.LENT,
                amount = TextFieldValue(value = "5000", valueError = false, onValueChange = {}),
                linkedToAccount = true,
                dueDate = null,
                isSettled = false,
                currency = Currency("$", "USD"),
                selectedAccount = AccountUiModel(
                    id = "1",
                    name = "Wallet",
                    storedIcon = StoredIcon("ic_calendar", "#000000"),
                    amount = Amount(0.0, "$ 0.00"),
                    amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
                ),
                accounts = emptyList(),
                remainingAmount = null,
                showDeleteButton = false,
                showDeleteDialog = false,
                showAccountSelection = false,
                showDueDateSelection = false,
            ),
            onAction = {},
        )
    }
}
