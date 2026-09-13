package com.naveenapps.expensemanager.feature.worktime.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.model.WorkTimePeriod
import com.naveenapps.expensemanager.feature.category.selection.MultipleCategoriesSelectionScreen
import com.naveenapps.expensemanager.feature.worktime.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkTimeSettingsScreen(
    viewModel: WorkTimeSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    WorkTimeSettingsContent(state = state, onAction = viewModel::processAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkTimeSettingsContent(
    state: WorkTimeSettingsState,
    onAction: (WorkTimeSettingsAction) -> Unit,
) {
    if (state.showCategorySelection) {
        SafeModalBottomSheet(
            onDismissRequest = { onAction(WorkTimeSettingsAction.DismissCategorySelection) },
        ) {
            MultipleCategoriesSelectionScreen(
                selectedCategories = state.selectedCategories,
                onItemSelection = { items, _ ->
                    onAction(WorkTimeSettingsAction.SetCategories(items))
                },
            )
        }
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction(WorkTimeSettingsAction.ClosePage) },
                title = stringResource(R.string.work_time_settings_title),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(WorkTimeSettingsAction.Save) },
                text = { Text(stringResource(R.string.work_time_save)) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.errorMessage?.let { message ->
                AppCardView(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // ── Section Revenu ────────────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.work_time_income_label),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                AppCardView {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DecimalTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.income.value,
                            isError = state.income.valueError,
                            onValueChange = state.income.onValueChange,
                            label = R.string.work_time_income_label,
                            errorMessage = "",
                        )

                        Text(
                            text = stringResource(R.string.work_time_income_period_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val periods = WorkTimePeriod.entries
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            periods.forEachIndexed { index, period ->
                                SegmentedButton(
                                    selected = state.selectedPeriod == period,
                                    onClick = { onAction(WorkTimeSettingsAction.SetPeriod(period)) },
                                    shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                                    icon = {},
                                    label = {
                                        Text(
                                            text = when (period) {
                                                WorkTimePeriod.HOUR -> stringResource(R.string.work_time_period_hour)
                                                WorkTimePeriod.DAY -> stringResource(R.string.work_time_period_day)
                                                WorkTimePeriod.WEEK -> stringResource(R.string.work_time_period_week)
                                                WorkTimePeriod.MONTH -> stringResource(R.string.work_time_period_month)
                                            },
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── Section Rythme de travail ─────────────────────────────────────
            SettingsSection(title = stringResource(R.string.work_time_hours_per_day_label)) {
                AppCardView {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DecimalTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.hoursPerDay.value,
                            isError = state.hoursPerDay.valueError,
                            onValueChange = state.hoursPerDay.onValueChange,
                            label = R.string.work_time_hours_per_day_label,
                            errorMessage = stringResource(R.string.work_time_error_hours_per_day),
                        )

                        DecimalTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.daysPerWeek.value,
                            isError = state.daysPerWeek.valueError,
                            onValueChange = state.daysPerWeek.onValueChange,
                            label = R.string.work_time_days_per_week_label,
                            errorMessage = stringResource(R.string.work_time_error_days_per_week),
                        )
                    }
                }
            }

            // ── Section Catégories pour l'auto-détection ─────────────────────
            SettingsSection(title = stringResource(R.string.work_time_auto_detect_categories_label)) {
                AppCardView {
                    Column {
                        SettingRow(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.work_time_auto_detect_categories_label),
                            icon = Icons.Default.Category,
                            value = state.selectedCategories.size.toString(),
                            onClick = {
                                onAction(WorkTimeSettingsAction.ShowCategorySelection)
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

