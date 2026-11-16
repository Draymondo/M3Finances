package com.naveenapps.expensemanager.feature.installmentplan.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
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
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.account.selection.AccountItem
import com.naveenapps.expensemanager.feature.account.selection.AccountItemDefaults
import com.naveenapps.expensemanager.feature.account.selection.AccountSelectionScreen
import com.naveenapps.expensemanager.feature.installmentplan.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun InstallmentPlanCreateScreen(
    viewModel: InstallmentPlanCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    InstallmentPlanCreateScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun InstallmentPlanCreateScreenContent(
    state: InstallmentPlanCreateState,
    onAction: (InstallmentPlanCreateAction) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = { onAction.invoke(InstallmentPlanCreateAction.Delete) },
            dismiss = { onAction.invoke(InstallmentPlanCreateAction.DismissDeleteDialog) },
        )
    } else if (state.showPaymentAccountSelection) {
        PaymentAccountSelectionView(state, onAction)
    } else if (state.showPaymentSheet) {
        PaymentSheetView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(InstallmentPlanCreateAction.ClosePage) },
                title = if (state.showDeleteButton) {
                    stringResource(R.string.edit_installment_plan)
                } else {
                    stringResource(R.string.create_installment_plan)
                },
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(
                            onClick = { onAction.invoke(InstallmentPlanCreateAction.ShowDeleteDialog) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_installment_plan),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(InstallmentPlanCreateAction.Save) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                text = { Text(text = stringResource(R.string.save)) },
            )
        },
    ) { innerPadding ->
        InstallmentPlanCreateBody(
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
private fun PaymentAccountSelectionView(
    state: InstallmentPlanCreateState,
    onAction: (InstallmentPlanCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(InstallmentPlanCreateAction.DismissPaymentAccountSelection) },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = state.paymentAccount,
            onItemSelection = { onAction.invoke(InstallmentPlanCreateAction.SelectPaymentAccount(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PaymentSheetView(
    state: InstallmentPlanCreateState,
    onAction: (InstallmentPlanCreateAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(InstallmentPlanCreateAction.DismissPaymentSheet) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.record_payment),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFilterChip(
                    filterName = stringResource(R.string.payment),
                    isSelected = !state.isRefund,
                    onClick = {
                        if (state.isRefund) {
                            onAction.invoke(InstallmentPlanCreateAction.TogglePaymentDirection)
                        }
                    },
                )
                AppFilterChip(
                    filterName = stringResource(R.string.refund),
                    isSelected = state.isRefund,
                    onClick = {
                        if (!state.isRefund) {
                            onAction.invoke(InstallmentPlanCreateAction.TogglePaymentDirection)
                        }
                    },
                )
            }

            DecimalTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.paymentAmount.value,
                isError = state.paymentAmount.valueError,
                onValueChange = state.paymentAmount.onValueChange,
                label = R.string.payment_amount,
                errorMessage = stringResource(R.string.amount_error),
            )

            state.paymentAccount?.let { paymentAccount ->
                AccountItem(
                    name = paymentAccount.name,
                    icon = paymentAccount.storedIcon.name,
                    iconBackgroundColor = paymentAccount.storedIcon.backgroundColor,
                    amount = paymentAccount.amount.amountString,
                    amountTextColor = paymentAccount.amountTextColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(InstallmentPlanCreateAction.ShowPaymentAccountSelection)
                    },
                    trailingContent = { AccountItemDefaults.ChevronTrailing() },
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction.invoke(InstallmentPlanCreateAction.ConfirmPayment) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    }
}

private fun daysBetween(from: Date, to: Date): Long =
    TimeUnit.MILLISECONDS.toDays(abs(to.time - from.time))

@Composable
private fun InstallmentPlanCreateBody(
    state: InstallmentPlanCreateState,
    onAction: (InstallmentPlanCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    if (state.showDeadlineSelection) {
        AppDatePickerDialog(
            selectedDate = state.deadline ?: java.util.Date(),
            onDateSelected = { onAction.invoke(InstallmentPlanCreateAction.SelectDeadline(it)) },
            onDismiss = { onAction.invoke(InstallmentPlanCreateAction.DismissDeadlineSelection) },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Paid-so-far summary + progress + "record a payment" entry point — only meaningful
        // once the plan exists.
        if (state.isEditing) {
            val now = Date()
            val completed = state.isCompleted || state.progress >= 1f
            val deadline = state.deadline
            val countdownText = deadline?.let { d ->
                when {
                    completed -> stringResource(R.string.deadline_on, d.toCompleteDateWithDate())
                    d.before(now) -> stringResource(R.string.overdue_by, daysBetween(d, now))
                    else -> stringResource(R.string.days_left, daysBetween(now, d))
                }
            }

            SettingsSection(
                title = stringResource(R.string.paid_amount),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                AppCardView {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.paid_so_far),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = state.paidAmount?.amountString.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.left_to_pay),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.remainingAmount?.amountString.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = androidx.compose.ui.res.colorResource(
                                id = if (completed) {
                                    com.naveenapps.expensemanager.core.common.R.color.green_500
                                } else {
                                    com.naveenapps.expensemanager.core.common.R.color.orange_500
                                },
                            ),
                            strokeCap = StrokeCap.Round,
                        )
                        if (countdownText != null) {
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!completed && deadline?.before(now) == true) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (!completed && state.accounts.isNotEmpty()) {
                            TextButton(
                                modifier = Modifier.padding(top = 4.dp),
                                onClick = { onAction.invoke(InstallmentPlanCreateAction.ShowPaymentSheet) },
                            ) {
                                Text(text = stringResource(R.string.record_payment))
                            }
                        }
                    }
                }
            }
        }

        SettingsSection(
            title = stringResource(R.string.details),
            modifier = Modifier.padding(top = 8.dp),
        ) {
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
                        label = R.string.plan_name,
                        errorMessage = stringResource(R.string.plan_name_error),
                    )

                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.targetAmount.value,
                        isError = state.targetAmount.valueError,
                        onValueChange = state.targetAmount.onValueChange,
                        label = R.string.target_price,
                        errorMessage = stringResource(R.string.amount_error),
                    )

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

        SettingsSection(title = stringResource(R.string.deadline)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClickableTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.deadline?.toCompleteDateWithDate()
                            ?: stringResource(R.string.no_deadline),
                        label = R.string.deadline,
                        leadingIcon = Icons.Outlined.EditCalendar,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAction.invoke(InstallmentPlanCreateAction.ShowDeadlineSelection)
                        },
                    )
                    if (state.deadline != null) {
                        TextButton(onClick = { onAction.invoke(InstallmentPlanCreateAction.ClearDeadline) }) {
                            Text(text = stringResource(R.string.clear))
                        }
                    }
                }
            }
        }

        if (state.isEditing) {
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
                            text = stringResource(R.string.completed),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.completed_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isCompleted,
                        onCheckedChange = { onAction.invoke(InstallmentPlanCreateAction.ToggleCompleted) },
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.payment_history)) {
                AppCardView {
                    if (state.payments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_payments_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            state.payments.forEach { payment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = payment.date.toCompleteDateWithDate(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        if (payment.isRefund) {
                                            Text(
                                                text = stringResource(R.string.refund_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                    Text(
                                        text = (if (payment.isRefund) "−" else "+") +
                                            (payment.amount.amountString.orEmpty()),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (payment.isRefund) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                                if (payment != state.payments.last()) {
                                    androidx.compose.material3.HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun InstallmentPlanCreateScreenPreview() {
    NaveenAppsPreviewTheme {
        InstallmentPlanCreateScreenContent(
            state = InstallmentPlanCreateState(
                isEditing = false,
                name = TextFieldValue(value = "Téléphone", valueError = false, onValueChange = {}),
                notes = TextFieldValue(value = "", valueError = false, onValueChange = {}),
                targetAmount = TextFieldValue(value = "300000", valueError = false, onValueChange = {}),
                deadline = null,
                isCompleted = false,
                currency = Currency("CFA", "XOF"),
                accounts = emptyList(),
                paidAmount = null,
                remainingAmount = null,
                progress = 0f,
                payments = emptyList(),
                showDeleteButton = false,
                showDeleteDialog = false,
                showDeadlineSelection = false,
            ),
            onAction = {},
        )
    }
}
