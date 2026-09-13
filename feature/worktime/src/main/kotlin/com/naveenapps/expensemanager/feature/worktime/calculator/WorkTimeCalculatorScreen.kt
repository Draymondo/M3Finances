package com.naveenapps.expensemanager.feature.worktime.calculator

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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.model.WorkTimeUnit
import com.naveenapps.expensemanager.feature.worktime.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Locale
import kotlin.math.ceil

@Composable
fun WorkTimeCalculatorScreen(
    viewModel: WorkTimeCalculatorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    WorkTimeCalculatorContent(state = state, onAction = viewModel::processAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkTimeCalculatorContent(
    state: WorkTimeCalculatorState,
    onAction: (WorkTimeCalculatorAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { /* handled by nav stack */ },
                title = stringResource(R.string.work_time_calculator_title),
                actions = {
                    IconButton(onClick = { onAction(WorkTimeCalculatorAction.OpenSettings) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.work_time_open_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(WorkTimeCalculatorAction.Calculate) },
                text = { Text(stringResource(R.string.work_time_calculate)) },
                icon = {},
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
            // ── Avertissement si les réglages ne sont pas configurés ──────────
            if (!state.isSettingsConfigured) {
                AppCardView(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.work_time_no_settings_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // ── Switch auto-détection ─────────────────────────────────────────
            AppCardView(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.work_time_auto_detect_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (state.autoDetectEnabled) {
                                stringResource(R.string.work_time_auto_detect_enabled_desc)
                            } else {
                                stringResource(R.string.work_time_auto_detect_disabled_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.autoDetectEnabled,
                        onCheckedChange = { onAction(WorkTimeCalculatorAction.ToggleAutoDetect(it)) },
                    )
                }
            }

            // ── Champ prix ────────────────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.work_time_price_label),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                AppCardView {
                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        value = state.price.value,
                        isError = state.price.valueError,
                        onValueChange = state.price.onValueChange,
                        label = R.string.work_time_price_label,
                        errorMessage = "",
                    )
                }
            }

            // ── Sélecteur d'unité de sortie ───────────────────────────────────
            SettingsSection(title = stringResource(R.string.work_time_output_unit_label)) {
                val units = WorkTimeUnit.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    units.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = state.selectedUnit == unit,
                            onClick = { onAction(WorkTimeCalculatorAction.SetUnit(unit)) },
                            shape = SegmentedButtonDefaults.itemShape(index, units.size),
                            icon = {},
                            label = {
                                Text(
                                    text = when (unit) {
                                        WorkTimeUnit.MINUTE -> stringResource(R.string.work_time_unit_minute)
                                        WorkTimeUnit.HOUR -> stringResource(R.string.work_time_unit_hour)
                                        WorkTimeUnit.DAY -> stringResource(R.string.work_time_unit_day)
                                        WorkTimeUnit.MONTH -> stringResource(R.string.work_time_unit_month)
                                    }
                                )
                            },
                        )
                    }
                }
            }

            // ── Message d'erreur ─────────────────────────────────────────────
            state.errorMessage?.let { message ->
                AppCardView(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // ── Résultat (arrondi au supérieur) ──────────────────────────────
            state.result?.let { value ->
                AppCardView(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.work_time_result_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatWorkTimeValue(value, state.selectedUnit),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(
                                text = stringResource(getWorkTimeUnitString(state.selectedUnit, value)).lowercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun formatWorkTimeValue(value: Double, unit: WorkTimeUnit): String {
    return if (unit == WorkTimeUnit.MONTH) {
        if (value < 1.0) {
            val rounded = ceil(value * 10.0) / 10.0
            String.format(Locale.getDefault(), "%.1f", rounded)
        } else {
            val intVal = ceil(value).toLong()
            String.format(Locale.getDefault(), "%,d", intVal)
        }
    } else {
        val intVal = ceil(value).toLong()
        String.format(Locale.getDefault(), "%,d", intVal)
    }
}

private fun getWorkTimeUnitString(unit: WorkTimeUnit, value: Double): Int {
    val isPlural = ceil(value) > 1.0
    return when (unit) {
        WorkTimeUnit.MINUTE -> if (isPlural) R.string.work_time_unit_minutes else R.string.work_time_unit_minute
        WorkTimeUnit.HOUR -> if (isPlural) R.string.work_time_unit_hours else R.string.work_time_unit_hour
        WorkTimeUnit.DAY -> if (isPlural) R.string.work_time_unit_days else R.string.work_time_unit_day
        WorkTimeUnit.MONTH -> if (isPlural) R.string.work_time_unit_months else R.string.work_time_unit_month
    }
}
