package com.naveenapps.expensemanager.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.EditNotifications
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.common.utils.toCompleteDate
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingToggleRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.designsystem.utils.ObserveAsEvents
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.repository.BackupRepository
import com.naveenapps.expensemanager.core.repository.ShareRepository
import com.naveenapps.expensemanager.feature.language.LanguageDialogView
import com.naveenapps.expensemanager.feature.theme.ThemeDialogView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    shareRepository: ShareRepository,
    backupRepository: BackupRepository,
    viewModel: SettingsViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsState()

    ObserveAsEvents(viewModel.event) {
        when (it) {
            SettingEvent.RateUs -> {
                shareRepository.openRateUs()
            }

            SettingEvent.Backup -> {
                backupRepository.backupData(null)
            }

            SettingEvent.Restore -> {
                backupRepository.restoreData(null)
            }
        }
    }

    SettingsScreenScaffoldView(
        state = state,
        onAction = viewModel::processAction
    )
}

@Composable
private fun SettingsScreenScaffoldView(
    state: SettingState,
    onAction: (SettingAction) -> Unit,
) {
    if (state.showThemeSelection) {
        ThemeDialogView {
            onAction.invoke(SettingAction.DismissThemeSelection)
        }
    }

    if (state.showLanguageSelection) {
        LanguageDialogView {
            onAction.invoke(SettingAction.DismissLanguageSelection)
        }
    }

    if (state.showSnapshotPicker) {
        SnapshotPickerDialog(
            dates = state.snapshotDates,
            onPick = { onAction.invoke(SettingAction.RestoreFromSnapshot(it)) },
            onDismiss = { onAction.invoke(SettingAction.DismissSnapshotPicker) },
        )
    }

    if (state.showCloudSyncConflict) {
        CloudSyncConflictDialog(
            onUseCloud = { onAction.invoke(SettingAction.UseCloudData) },
            onKeepLocal = { onAction.invoke(SettingAction.KeepPhoneData) },
            resolving = state.isCloudConflictResolving,
        )
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = {
                    onAction.invoke(SettingAction.ClosePage)
                },
                title = stringResource(R.string.settings),
            )
        },
    ) { innerPadding ->
        SettingsScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    state: SettingState,
    onAction: (SettingAction) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        // Personalization — shapes how the app looks and feels, so it comes first.
        SettingsSection(title = stringResource(R.string.personalization)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.ShowThemeSelection) },
                    title = stringResource(id = R.string.theme),
                    subtitle = if (state.theme != null) {
                        stringResource(id = state.theme.titleResId)
                    } else {
                        stringResource(id = R.string.system_default)
                    },
                    icon = Icons.Outlined.Palette,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.ShowLanguageSelection) },
                    title = stringResource(
                        id = com.naveenapps.expensemanager.feature.language.R.string.language,
                    ),
                    subtitle = if (state.locale != null) {
                        stringResource(id = state.locale.titleResId)
                    } else {
                        stringResource(id = R.string.system_default)
                    },
                    icon = Icons.Outlined.Language,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenCurrencyEdit) },
                    title = stringResource(id = R.string.currency),
                    subtitle = "${state.currency.name} (${state.currency.symbol})",
                    icon = Icons.Outlined.Payments,
                    showDivider = true,
                )
                SettingToggleRow(
                    title = stringResource(id = R.string.compact_summary),
                    subtitle = stringResource(id = R.string.compact_summary_message),
                    icon = Icons.Outlined.DashboardCustomize,
                    checked = state.isCompactSummary,
                    onCheckedChange = { onAction.invoke(SettingAction.ToggleCompactSummary) },
                )
            }
        }

        // Tools & Features — central tools for tracking finances.
        SettingsSection(title = stringResource(id = R.string.tools_and_features)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenRecurringTransactions) },
                    title = stringResource(id = R.string.recurring_transactions_settings),
                    subtitle = stringResource(id = R.string.recurring_transactions_settings_subtitle),
                    icon = Icons.Outlined.Autorenew,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenDebts) },
                    title = stringResource(id = R.string.debts_settings),
                    subtitle = stringResource(id = R.string.debts_settings_subtitle),
                    icon = Icons.Outlined.Handshake,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenSavingsGoals) },
                    title = stringResource(id = R.string.savings_goals_settings),
                    subtitle = stringResource(id = R.string.savings_goals_settings_subtitle),
                    icon = Icons.Outlined.Savings,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenShoppingLists) },
                    title = stringResource(id = R.string.shopping_lists_settings),
                    subtitle = stringResource(id = R.string.shopping_lists_settings_subtitle),
                    icon = Icons.Outlined.ShoppingCart,
                )
            }
        }

        // Notifications & Alerts
        SettingsSection(title = stringResource(R.string.notifications)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenNotification) },
                    title = stringResource(id = R.string.reminder_notification),
                    subtitle = stringResource(id = R.string.selected_daily_reminder_time),
                    icon = Icons.Outlined.EditNotifications,
                )
            }
        }

        // Defaults — affects daily data entry.
        SettingsSection(title = stringResource(R.string.defaults)) {
            AppCardView {
                if (state.accounts.isNotEmpty() && state.selectedAccount != null) {
                    DropdownSettingItem(
                        label = stringResource(id = R.string.default_account),
                        selectedValue = state.selectedAccount.name,
                        icon = Icons.Outlined.AccountBalance,
                        items = state.accounts.map { it.name },
                        onItemSelected = { index ->
                            onAction.invoke(SettingAction.SelectAccount(state.accounts[index]))
                        },
                    )
                }

                if (state.expenseCategories.isNotEmpty() && state.selectedExpenseCategory != null) {
                    DropdownSettingItem(
                        label = stringResource(id = R.string.default_expense_category),
                        selectedValue = state.selectedExpenseCategory.titleResId?.let {
                            stringResource(it)
                        } ?: state.selectedExpenseCategory.name,
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        items = state.expenseCategories.map {
                            it.titleResId?.let { resId -> stringResource(resId) } ?: it.name
                        },
                        onItemSelected = { index ->
                            onAction.invoke(SettingAction.SelectExpenseCategory(state.expenseCategories[index]))
                        },
                    )
                }

                if (state.incomeCategories.isNotEmpty() && state.selectedIncomeCategory != null) {
                    DropdownSettingItem(
                        label = stringResource(id = R.string.default_income_category),
                        selectedValue = state.selectedIncomeCategory.titleResId?.let {
                            stringResource(it)
                        } ?: state.selectedIncomeCategory.name,
                        icon = Icons.AutoMirrored.Outlined.TrendingDown,
                        items = state.incomeCategories.map {
                            it.titleResId?.let { resId -> stringResource(resId) } ?: it.name
                        },
                        onItemSelected = { index ->
                            onAction.invoke(SettingAction.SelectIncomeCategory(state.incomeCategories[index]))
                        },
                    )
                }
            }
        }

        // Data & Backup — export and cloud sync options grouped together.
        SettingsSection(title = stringResource(R.string.data_and_backup)) {
            AppCardView {
                // Cloud backup toggle
                SettingToggleRow(
                    title = stringResource(id = R.string.cloud_backup),
                    subtitle = when {
                        state.isCloudSyncInProgress -> stringResource(id = R.string.cloud_backup_syncing)
                        state.showCloudSyncConflict -> stringResource(id = R.string.cloud_sync_conflict_pending)
                        state.cloudBackupErrorMessage != null -> state.cloudBackupErrorMessage
                        state.isCloudBackupConnected && state.lastCloudBackupTime != null ->
                            stringResource(
                                id = R.string.cloud_backup_last_synced,
                                state.lastCloudBackupTime.toCompleteDate(),
                            )

                        state.isCloudBackupConnected -> stringResource(id = R.string.cloud_backup_connected)
                        else -> stringResource(id = R.string.cloud_backup_message)
                    },
                    icon = Icons.Outlined.CloudSync,
                    checked = state.isCloudBackupConnected,
                    onCheckedChange = {
                        if (!state.isCloudSyncInProgress) {
                            onAction.invoke(
                                if (state.isCloudBackupConnected) {
                                    SettingAction.DisconnectCloudBackup
                                } else {
                                    SettingAction.ConnectCloudBackup
                                },
                            )
                        }
                    },
                    showDivider = true,
                )
                if (state.isCloudBackupConnected) {
                    SettingRow(
                        onClick = {
                            if (!state.isCloudRestoreInProgress) {
                                onAction.invoke(SettingAction.RestoreFromCloud)
                            }
                        },
                        title = "Restaurer depuis le cloud",
                        subtitle = when {
                            state.isCloudRestoreInProgress -> "Restauration en cours…"
                            state.cloudRestoreStatusMessage != null -> state.cloudRestoreStatusMessage
                            else -> "Récupère tes données depuis la dernière sauvegarde cloud"
                        },
                        icon = Icons.Outlined.CloudDownload,
                        showDivider = true,
                    )
                }
                if (state.isCloudBackupConnected) {
                    SettingRow(
                        onClick = { onAction.invoke(SettingAction.ShowSnapshotPicker) },
                        title = "Sauvegardes datées",
                        subtitle = "Restaurer un jour précis (7 derniers jours)",
                        icon = Icons.Outlined.History,
                        showDivider = true,
                    )
                }

                // Export row
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenExport) },
                    title = stringResource(id = R.string.export),
                    subtitle = stringResource(id = R.string.export_message),
                    icon = Icons.Outlined.Upload,
                )
            }
        }

        // Security
        SettingsSection(title = stringResource(R.string.security)) {
            AppCardView {
                SettingToggleRow(
                    title = stringResource(id = R.string.app_lock),
                    subtitle = stringResource(id = R.string.app_lock_message),
                    icon = Icons.Outlined.Lock,
                    checked = state.isAppLockEnabled,
                    onCheckedChange = { onAction.invoke(SettingAction.ToggleAppLock) },
                )
            }
        }

        // Intelligence Artificielle
        SettingsSection(title = "Intelligence Artificielle") {
            AppCardView {
                var showGeminiDialog by remember { mutableStateOf(false) }
                SettingRow(
                    onClick = { showGeminiDialog = true },
                    title = "Clé API Gemini",
                    subtitle = if (state.geminiApiKey.isBlank()) {
                        "Non configurée (cliquer pour ajouter)"
                    } else {
                        "••••••••••••••••"
                    },
                    icon = Icons.Outlined.Info,
                )

                if (showGeminiDialog) {
                    GeminiApiKeyDialog(
                        currentKey = state.geminiApiKey,
                        onSave = {
                            onAction.invoke(SettingAction.ChangeGeminiApiKey(it))
                            showGeminiDialog = false
                        },
                        onDismiss = { showGeminiDialog = false }
                    )
                }
            }
        }

        // Advanced
        SettingsSection(title = stringResource(R.string.advanced)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenAdvancedSettings) },
                    title = stringResource(id = R.string.advanced),
                    subtitle = stringResource(id = R.string.advanced_config_message),
                    icon = Icons.Outlined.SettingsApplications,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SnapshotPickerDialog(
    dates: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        AppCardView(modifier = Modifier.padding(16.dp)) {
            LazyColumn(
                modifier = Modifier.wrapContentSize(),
            ) {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        text = "Choisir une sauvegarde datée",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (dates.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            text = "Aucune sauvegarde datée disponible pour l'instant",
                        )
                    }
                }
                items(dates) { date ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(date) }
                            .padding(16.dp),
                    ) {
                        Text(text = date)
                    }
                }
            }
        }
    }
}

// Moved here from AdvancedSettingsScreen along with the Defaults section it belongs to.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingItem(
    label: String,
    selectedValue: String,
    icon: ImageVector,
    items: List<String>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            fontWeight = if (item == selectedValue) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = if (item == selectedValue) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else null,
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}


@AppPreviewsLightAndDarkMode
@Composable
fun SettingsScreenPreview() {
    NaveenAppsPreviewTheme(padding = 0.dp) {
        SettingsScreenScaffoldView(
            state = SettingState(
                currency = Currency("$", "US Dollar"),
                theme = null,
                showThemeSelection = false
            ),
            onAction = {}
        )
    }
}

@Composable
private fun GeminiApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(currentKey) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configuration de la clé Gemini",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pour utiliser le scan de tickets de caisse par IA, saisissez votre clé d'API Google Gemini ci-dessous.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Clé d'API Gemini") },
                    singleLine = true,
                    placeholder = { Text("AIzaSy...") }
                )
                Text(
                    modifier = Modifier
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://aistudio.google.com/")
                            )
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    text = "Obtenir une clé d'API gratuite sur Google AI Studio ↗",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key) }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
