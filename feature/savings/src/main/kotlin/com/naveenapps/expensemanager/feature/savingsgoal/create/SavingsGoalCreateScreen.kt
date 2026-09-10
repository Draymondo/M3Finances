package com.naveenapps.expensemanager.feature.savingsgoal.create

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.account.selection.AccountItem
import com.naveenapps.expensemanager.feature.account.selection.AccountItemDefaults
import com.naveenapps.expensemanager.feature.account.selection.AccountSelectionScreen
import com.naveenapps.expensemanager.feature.savingsgoal.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SavingsGoalCreateScreen(
    viewModel: SavingsGoalCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    SavingsGoalCreateScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun SavingsGoalCreateScreenContent(
    state: SavingsGoalCreateState,
    onAction: (SavingsGoalCreateAction) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = { onAction.invoke(SavingsGoalCreateAction.Delete) },
            dismiss = { onAction.invoke(SavingsGoalCreateAction.DismissDeleteDialog) },
        )
    } else if (state.showAccountSelection) {
        AccountSelectionView(state, onAction)
    } else if (state.showContributionAccountSelection) {
        ContributionAccountSelectionView(state, onAction)
    } else if (state.showContributionSheet) {
        ContributionSheetView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(SavingsGoalCreateAction.ClosePage) },
                title = if (state.showDeleteButton) {
                    stringResource(R.string.edit_savings_goal)
                } else {
                    stringResource(R.string.create_savings_goal)
                },
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(
                            onClick = { onAction.invoke(SavingsGoalCreateAction.ShowDeleteDialog) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_savings_goal),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(SavingsGoalCreateAction.Save) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                text = { Text(text = stringResource(R.string.save)) },
            )
        },
    ) { innerPadding ->
        SavingsGoalCreateBody(
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
    state: SavingsGoalCreateState,
    onAction: (SavingsGoalCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(SavingsGoalCreateAction.DismissAccountSelection) },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = state.selectedAccount,
            createNewCallback = { onAction.invoke(SavingsGoalCreateAction.OpenAccountCreate) },
            onItemSelection = { onAction.invoke(SavingsGoalCreateAction.SelectAccount(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ContributionAccountSelectionView(
    state: SavingsGoalCreateState,
    onAction: (SavingsGoalCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(SavingsGoalCreateAction.DismissContributionAccountSelection) },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = state.contributionAccount,
            createNewCallback = { onAction.invoke(SavingsGoalCreateAction.OpenAccountCreate) },
            onItemSelection = { onAction.invoke(SavingsGoalCreateAction.SelectContributionAccount(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ContributionSheetView(
    state: SavingsGoalCreateState,
    onAction: (SavingsGoalCreateAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(SavingsGoalCreateAction.DismissContributionSheet) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.record_contribution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFilterChip(
                    filterName = stringResource(R.string.contribution),
                    isSelected = !state.isWithdrawal,
                    onClick = {
                        if (state.isWithdrawal) {
                            onAction.invoke(SavingsGoalCreateAction.ToggleContributionDirection)
                        }
                    },
                )
                AppFilterChip(
                    filterName = stringResource(R.string.withdrawal),
                    isSelected = state.isWithdrawal,
                    onClick = {
                        if (!state.isWithdrawal) {
                            onAction.invoke(SavingsGoalCreateAction.ToggleContributionDirection)
                        }
                    },
                )
            }

            DecimalTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.contributionAmount.value,
                isError = state.contributionAmount.valueError,
                onValueChange = state.contributionAmount.onValueChange,
                label = R.string.contribution_amount,
                errorMessage = stringResource(R.string.amount_error),
            )

            state.contributionAccount?.let { contributionAccount ->
                AccountItem(
                    name = contributionAccount.name,
                    icon = contributionAccount.storedIcon.name,
                    iconBackgroundColor = contributionAccount.storedIcon.backgroundColor,
                    amount = contributionAccount.amount.amountString,
                    amountTextColor = contributionAccount.amountTextColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(SavingsGoalCreateAction.ShowContributionAccountSelection)
                    },
                    trailingContent = { AccountItemDefaults.ChevronTrailing() },
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction.invoke(SavingsGoalCreateAction.ConfirmContribution) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun SavingsGoalCreateBody(
    state: SavingsGoalCreateState,
    onAction: (SavingsGoalCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    if (state.showTargetDateSelection) {
        AppDatePickerDialog(
            selectedDate = state.targetDate ?: java.util.Date(),
            onDateSelected = { onAction.invoke(SavingsGoalCreateAction.SelectTargetDate(it)) },
            onDismiss = { onAction.invoke(SavingsGoalCreateAction.DismissTargetDateSelection) },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Saved-so-far summary + "add contribution" entry point — fixed once created, since a
        // new goal has nowhere to attach a contribution to until it's actually saved.
        if (state.isEditing) {
            SettingsSection(
                title = stringResource(R.string.saved_amount),
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
                            text = stringResource(R.string.saved_so_far),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = state.savedAmount?.amountString.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (!state.isAchieved && state.accounts.isNotEmpty()) {
                        TextButton(
                            modifier = Modifier.padding(bottom = 8.dp),
                            onClick = { onAction.invoke(SavingsGoalCreateAction.ShowContributionSheet) },
                        ) {
                            Text(text = stringResource(R.string.record_contribution))
                        }
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.details), modifier = Modifier.padding(top = 8.dp)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StringTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.name.value,
                        isError = state.name.valueError,
                        onValueChange = state.name.onValueChange,
                        label = R.string.savings_goal_name,
                        errorMessage = stringResource(R.string.savings_goal_name_error),
                    )

                    Text(
                        text = stringResource(R.string.savings_strategy),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilterChip(
                            filterName = stringResource(R.string.strategy_fixed),
                            isSelected = state.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.FIXED,
                            onClick = {
                                if (state.savingsStrategy != com.naveenapps.expensemanager.core.model.SavingsStrategy.FIXED) {
                                    onAction.invoke(SavingsGoalCreateAction.ToggleSavingsStrategy)
                                }
                            }
                        )
                        AppFilterChip(
                            filterName = stringResource(R.string.strategy_percentage),
                            isSelected = state.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME,
                            onClick = {
                                if (state.savingsStrategy != com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME) {
                                    onAction.invoke(SavingsGoalCreateAction.ToggleSavingsStrategy)
                                }
                            }
                        )
                    }

                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.targetAmount.value,
                        isError = state.targetAmount.valueError,
                        onValueChange = state.targetAmount.onValueChange,
                        label = R.string.target_amount,
                        errorMessage = stringResource(R.string.amount_error),
                    )

                    if (state.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME) {
                        DecimalTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.targetPercentage.value,
                            isError = state.targetPercentage.valueError,
                            onValueChange = state.targetPercentage.onValueChange,
                            label = R.string.target_percentage,
                            errorMessage = stringResource(R.string.target_percentage_error),
                        )
                    }

                    if (!state.isEditing) {
                        DecimalTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.initialAmount.value,
                            isError = state.initialAmount.valueError,
                            onValueChange = state.initialAmount.onValueChange,
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

        // Funding account for the initial contribution — only shown while creating; the real
        // account is fixed afterward, baked into the already-recorded transfer (if any).
        AnimatedVisibility(
            visible = !state.isEditing,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            SettingsSection(title = stringResource(R.string.funding_account)) {
                AccountItem(
                    name = state.selectedAccount.name,
                    icon = state.selectedAccount.storedIcon.name,
                    iconBackgroundColor = state.selectedAccount.storedIcon.backgroundColor,
                    amount = state.selectedAccount.amount.amountString,
                    amountTextColor = state.selectedAccount.amountTextColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(SavingsGoalCreateAction.ShowAccountSelection)
                    },
                    trailingContent = { AccountItemDefaults.ChevronTrailing() },
                )
            }
        }

        SettingsSection(title = stringResource(R.string.target_date)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClickableTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.targetDate?.toCompleteDateWithDate()
                            ?: stringResource(R.string.no_target_date),
                        label = R.string.target_date,
                        leadingIcon = Icons.Outlined.EditCalendar,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAction.invoke(SavingsGoalCreateAction.ShowTargetDateSelection)
                        },
                    )
                    if (state.targetDate != null) {
                        TextButton(onClick = { onAction.invoke(SavingsGoalCreateAction.ClearTargetDate) }) {
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
                            text = stringResource(R.string.achieved),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.achieved_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isAchieved,
                        onCheckedChange = { onAction.invoke(SavingsGoalCreateAction.ToggleAchieved) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun SavingsGoalCreateScreenPreview() {
    NaveenAppsPreviewTheme {
        SavingsGoalCreateScreenContent(
            state = SavingsGoalCreateState(
                isEditing = false,
                name = TextFieldValue(value = "Vacances", valueError = false, onValueChange = {}),
                notes = TextFieldValue(value = "", valueError = false, onValueChange = {}),
                targetAmount = TextFieldValue(value = "2000", valueError = false, onValueChange = {}),
                initialAmount = TextFieldValue(value = "0", valueError = false, onValueChange = {}),
                targetDate = null,
                isAchieved = false,
                savingsStrategy = com.naveenapps.expensemanager.core.model.SavingsStrategy.FIXED,
                targetPercentage = TextFieldValue("", false, {}),
                currency = Currency("$", "USD"),
                selectedAccount = AccountUiModel(
                    id = "1",
                    name = "Wallet",
                    storedIcon = StoredIcon("ic_calendar", "#000000"),
                    amount = Amount(0.0, "$ 0.00"),
                    amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
                ),
                accounts = emptyList(),
                savedAmount = null,
                showDeleteButton = false,
                showDeleteDialog = false,
                showAccountSelection = false,
                showTargetDateSelection = false,
            ),
            onAction = {},
        )
    }
}
