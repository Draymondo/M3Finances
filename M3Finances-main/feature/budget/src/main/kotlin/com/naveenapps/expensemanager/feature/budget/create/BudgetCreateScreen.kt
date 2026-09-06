package com.naveenapps.expensemanager.feature.budget.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.common.utils.fromShortMonthAndYearToDate
import com.naveenapps.expensemanager.core.common.utils.fromYear
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.common.utils.toCompleteDate
import com.naveenapps.expensemanager.core.common.utils.toMonth
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYear
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.common.utils.toYearInt
import com.naveenapps.expensemanager.core.designsystem.components.DeleteDialogItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppDatePickerDialog
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppFilterChip
import com.naveenapps.expensemanager.core.designsystem.ui.components.ClickableTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.MonthPicker
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.designsystem.ui.components.YearPicker
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetEquivalentUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.account.selection.MultipleAccountSelectionScreen
import com.naveenapps.expensemanager.feature.budget.R
import com.naveenapps.expensemanager.feature.budget.list.BudgetItem
import com.naveenapps.expensemanager.feature.budget.periodLabelRes
import com.naveenapps.expensemanager.feature.category.selection.MultipleCategoriesSelectionScreen
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun BudgetCreateScreen(
    viewModel: BudgetCreateViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsState()

    BudgetCreateScreenContentView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCreateScreenContentView(
    state: BudgetCreateState,
    onAction: (BudgetCreateAction) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = {
                onAction.invoke(BudgetCreateAction.Delete)
            },
            dismiss = {
                onAction.invoke(BudgetCreateAction.ClosePage)
            },
        )
    }

    if (state.showAccountSelectionDialog) {
        SafeModalBottomSheet(
            onDismissRequest = {
                onAction.invoke(BudgetCreateAction.CloseAccountSelectionDialog)
            },
        ) {
            MultipleAccountSelectionScreen(
                selectedAccounts = state.selectedAccounts,
            ) { items, selected ->
                onAction.invoke(BudgetCreateAction.SelectAccounts(selected, items))
            }
        }
    }

    if (state.showCategorySelectionDialog) {
        SafeModalBottomSheet(
            onDismissRequest = {
                onAction.invoke(BudgetCreateAction.CloseCategorySelectionDialog)
            },
        ) {
            MultipleCategoriesSelectionScreen(
                selectedCategories = state.selectedCategories,
                onItemSelection = { items, selected ->
                    onAction.invoke(BudgetCreateAction.SelectCategories(selected, items))
                }
            )
        }
    }

    if (state.showMonthSelection) {
        if (state.periodType == BudgetPeriod.YEARLY) {
            YearPicker(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                    ),
                currentYear = state.month.value.toYearInt(),
                confirmButtonCLicked = { year ->
                    year.toString().fromYear()?.let {
                        state.month.onValueChange?.invoke(it)
                    } ?: run {
                        onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                    }
                },
                cancelClicked = {
                    onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                },
            )
        } else if (state.periodType == BudgetPeriod.MONTHLY) {
            MonthPicker(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                    ),
                currentMonth = state.month.value.toMonth(),
                currentYear = state.month.value.toYearInt(),
                confirmButtonCLicked = { month, year ->
                    ("$month-$year").fromShortMonthAndYearToDate()?.let {
                        state.month.onValueChange?.invoke(it)
                    } ?: run {
                        onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                    }
                },
                cancelClicked = {
                    onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                },
            )
        } else {
            // WEEKLY and DAILY both pick a single calendar date — a week budget's key is
            // derived from whichever Monday contains the picked date (see Date.toWeekKey), so
            // picking any day within the intended week is enough.
            AppDatePickerDialog(
                selectedDate = state.month.value,
                onDateSelected = {
                    state.month.onValueChange?.invoke(it)
                },
                onDismiss = {
                    onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                },
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = {
                    onAction.invoke(BudgetCreateAction.ClosePage)
                },
                title = if (state.showDeleteButton)
                    stringResource(R.string.edit_budget)
                else
                    stringResource(R.string.create_budget),
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(onClick = { onAction.invoke(BudgetCreateAction.ShowDeleteDialog) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(com.naveenapps.expensemanager.feature.category.R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(BudgetCreateAction.Save) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(text = stringResource(com.naveenapps.expensemanager.feature.account.R.string.save))
                },
            )
        },
    ) { innerPadding ->
        BudgetCreateScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            amountField = state.amount,
            currencyIconField = state.currency.symbol,
            selectedDate = state.month,
            periodType = state.periodType,
            goalType = state.goalType,
            accountCount = if (state.isAllAccountSelected) {
                stringResource(R.string.all)
            } else {
                state.selectedAccounts.size.toString()
            },
            categoriesCount = if (state.isAllCategorySelected) {
                stringResource(R.string.all)
            } else {
                state.selectedCategories.size.toString()
            },
            equivalents = state.equivalents,
            onAction = onAction,
        )
    }
}

@Composable
fun BudgetCreateScreen(
    amountField: TextFieldValue<String>,
    currencyIconField: String,
    selectedDate: TextFieldValue<Date>,
    accountCount: String,
    categoriesCount: String,
    onAction: (BudgetCreateAction) -> Unit,
    modifier: Modifier = Modifier,
    periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    goalType: com.naveenapps.expensemanager.core.model.BudgetGoalType = com.naveenapps.expensemanager.core.model.BudgetGoalType.EXPENSE,
    equivalents: List<BudgetEquivalentUiModel> = emptyList(),
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(
            title = stringResource(R.string.goal_type),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppFilterChip(
                    modifier = Modifier.weight(1f),
                    centerAlign = true,
                    filterName = stringResource(id = R.string.expense),
                    isSelected = goalType == com.naveenapps.expensemanager.core.model.BudgetGoalType.EXPENSE,
                    onClick = {
                        onAction.invoke(
                            BudgetCreateAction.SelectGoalType(com.naveenapps.expensemanager.core.model.BudgetGoalType.EXPENSE),
                        )
                    },
                )
                AppFilterChip(
                    modifier = Modifier.weight(1f),
                    centerAlign = true,
                    filterName = stringResource(id = R.string.income),
                    isSelected = goalType == com.naveenapps.expensemanager.core.model.BudgetGoalType.INCOME,
                    onClick = {
                        onAction.invoke(
                            BudgetCreateAction.SelectGoalType(com.naveenapps.expensemanager.core.model.BudgetGoalType.INCOME),
                        )
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(R.string.budget_for),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            AppCardView {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.monthly),
                            isSelected = periodType == BudgetPeriod.MONTHLY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.MONTHLY),
                                )
                            },
                        )
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.annual),
                            isSelected = periodType == BudgetPeriod.YEARLY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.YEARLY),
                                )
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.weekly),
                            isSelected = periodType == BudgetPeriod.WEEKLY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.WEEKLY),
                                )
                            },
                        )
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.daily),
                            isSelected = periodType == BudgetPeriod.DAILY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.DAILY),
                                )
                            },
                        )
                    }
                    ClickableTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = when (periodType) {
                            BudgetPeriod.YEARLY -> selectedDate.value.toYear()
                            BudgetPeriod.MONTHLY -> selectedDate.value.toMonthAndYear()
                            BudgetPeriod.WEEKLY -> {
                                val start = selectedDate.value.getStartOfTheWeek().toCompleteDate()
                                // getEndOfTheWeek is exclusive (start of the *next* week), so the
                                // displayed range shows the actual last millisecond-inclusive day.
                                val end = (selectedDate.value.getEndOfTheWeek() - 1).toCompleteDate()
                                "${start.toCompleteDateWithDate()} - ${end.toCompleteDateWithDate()}"
                            }
                            BudgetPeriod.DAILY -> selectedDate.value.toCompleteDateWithDate()
                        },
                        label = when (periodType) {
                            BudgetPeriod.YEARLY -> R.string.select_year
                            BudgetPeriod.MONTHLY -> R.string.select_month
                            BudgetPeriod.WEEKLY -> R.string.select_week
                            BudgetPeriod.DAILY -> R.string.select_day
                        },
                        leadingIcon = Icons.Default.EditCalendar,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAction.invoke(BudgetCreateAction.ShowMonthSelection)
                        },
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.what_is_your_budget_limit)) {
            AppCardView {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = amountField.value,
                        isError = amountField.valueError,
                        errorMessage = stringResource(id = R.string.budget_amount_error),
                        onValueChange = amountField.onValueChange,
                        label = R.string.budget_amount,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
            }
        }

        if (equivalents.isNotEmpty()) {
            SettingsSection(title = stringResource(R.string.budget_equivalents)) {
                AppCardView {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        equivalents.forEach { equivalent ->
                            BudgetItem(
                                name = stringResource(id = periodLabelRes(equivalent.periodType)),
                                progressBarColor = equivalent.progressBarColor,
                                amount = equivalent.equivalentAmount,
                                transactionAmount = equivalent.spentAmount,
                                percentage = equivalent.percent,
                            )
                        }
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.budget_scope)) {
            AppCardView {
                Column {
                    SettingRow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(id = R.string.select_account),
                        icon = Icons.Default.AccountBalance,
                        value = accountCount,
                        onClick = {
                            onAction.invoke(BudgetCreateAction.OpenAccountSelectionDialog)
                        },
                        showDivider = true
                    )
                    SettingRow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(id = R.string.select_category),
                        icon = Icons.Default.Category,
                        value = categoriesCount,
                        onClick = {
                            onAction.invoke(BudgetCreateAction.OpenCategorySelectionDialog)
                        }
                    )
                }
            }
        }


        // FAB clearance
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun BudgetCreateStatePreview() {
    val amountField = TextFieldValue(
        value = "0.0",
        valueError = false,
        onValueChange = { }
    )
    val dateField = TextFieldValue(
        value = Date(),
        valueError = false,
        onValueChange = { }
    )
    NaveenAppsPreviewTheme(padding = 0.dp) {
        BudgetCreateScreenContentView(
            state = BudgetCreateState(
                isLoading = false,
                amount = amountField,
                month = dateField,
                isAllCategorySelected = true,
                isAllAccountSelected = true,
                currency = Currency(symbol = "$", name = ""),
                showDeleteDialog = false,
                showDeleteButton = true,
                showAccountSelectionDialog = false,
                showCategorySelectionDialog = false,
                selectedCategories = emptyList(),
                selectedAccounts = emptyList(),
                showMonthSelection = false,
            ),
            onAction = {},
        )
    }
}
