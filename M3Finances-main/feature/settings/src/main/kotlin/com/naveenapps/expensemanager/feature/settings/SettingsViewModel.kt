package com.naveenapps.expensemanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.locale.GetCurrentLocaleUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.theme.GetCurrentThemeUseCase
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CloudSyncOutcome
import com.naveenapps.expensemanager.core.model.CloudSyncResolution
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SettingsViewModel(
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    getCurrencyUseCase: GetCurrencyUseCase,
    getCurrentThemeUseCase: GetCurrentThemeUseCase,
    getCurrentLocaleUseCase: GetCurrentLocaleUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    private val settingsRepository: SettingsRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val cloudBackupRepository: CloudBackupRepository,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _event = Channel<SettingEvent>()
    val event = _event.receiveAsFlow()

    private val _state = MutableStateFlow(
        SettingState(
            currency = getDefaultCurrencyUseCase.invoke(),
            theme = null,
            showThemeSelection = false
        )
    )
    val state = _state.asStateFlow()

    init {
        getCurrencyUseCase.invoke().onEach { currency ->
            _state.update { it.copy(currency = currency) }
        }.launchIn(viewModelScope)

        getCurrentThemeUseCase.invoke().onEach { theme ->
            _state.update { it.copy(theme = theme) }
        }.launchIn(viewModelScope)

        getCurrentLocaleUseCase.invoke().onEach { locale ->
            _state.update { it.copy(locale = locale) }
        }.launchIn(viewModelScope)

        // Defaults + Security section (moved here from AdvancedSettingsViewModel)
        settingsRepository.getHomeSummaryCompact().onEach { compact ->
            _state.update { it.copy(isCompactSummary = compact) }
        }.launchIn(viewModelScope)

        settingsRepository.isAppLockEnabled().onEach { enabled ->
            _state.update { it.copy(isAppLockEnabled = enabled) }
        }.launchIn(viewModelScope)

        getAllAccountsUseCase.invoke().onEach { accounts ->
            val accountId = settingsRepository.getDefaultAccount().firstOrNull()
            val account = accounts.find { it.id == accountId }
            _state.update {
                it.copy(
                    accounts = accounts,
                    selectedAccount = account ?: accounts.firstOrNull()
                )
            }
        }.launchIn(viewModelScope)

        getAllCategoryUseCase.invoke().onEach { categories ->
            val (expenses, incomes) = categories.partition { category -> category.type.isExpense() }

            val expenseCategoryId = settingsRepository.getDefaultExpenseCategory().firstOrNull()
            val expenseCategory = expenses.find { it.id == expenseCategoryId }

            val incomeCategoryId = settingsRepository.getDefaultIncomeCategory().firstOrNull()
            val incomeCategory = incomes.find { it.id == incomeCategoryId }

            _state.update {
                it.copy(
                    expenseCategories = expenses,
                    selectedExpenseCategory = expenseCategory ?: expenses.firstOrNull(),
                    incomeCategories = incomes,
                    selectedIncomeCategory = incomeCategory ?: incomes.firstOrNull(),
                )
            }
        }.launchIn(viewModelScope)

        googleAuthRepository.isSignedIn().onEach { isSignedIn ->
            _state.update { it.copy(isCloudBackupConnected = isSignedIn) }
            if (isSignedIn) {
                refreshLastCloudBackupTime()
            }
        }.launchIn(viewModelScope)

        cloudBackupRepository.observeUnresolvedConflict().onEach { conflict ->
            _state.update { it.copy(showCloudSyncConflict = conflict) }
        }.launchIn(viewModelScope)

        settingsRepository.getGeminiApiKey().onEach { key ->
            _state.update { it.copy(geminiApiKey = key) }
        }.launchIn(viewModelScope)
    }

    private fun refreshLastCloudBackupTime() {
        viewModelScope.launch {
            val lastBackupTime = cloudBackupRepository.getLastBackupTime()
            _state.update { it.copy(lastCloudBackupTime = lastBackupTime) }
        }
    }

    private fun connectCloudBackup() {
        viewModelScope.launch {
            _state.update { it.copy(isCloudSyncInProgress = true, cloudBackupErrorMessage = null) }

            when (val signInResult = googleAuthRepository.signInSilentlyOrPrompt()) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudSyncInProgress = false,
                            cloudBackupErrorMessage = signInResult.exception.message,
                        )
                    }
                    return@launch
                }

                is Resource.Success -> Unit
            }

            // First connection: push whatever is already on this device right away, rather than
            // waiting for the person's next edit to trigger the debounced background sync (see
            // DatabaseChangeCloudBackupTrigger) — otherwise the cloud would stay empty until
            // something changes locally, which would be a confusing "connected but nothing there
            // yet" state.
            when (val syncResult = cloudBackupRepository.syncAll()) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudSyncInProgress = false,
                            cloudBackupErrorMessage = syncResult.exception.message,
                        )
                    }
                }

                is Resource.Success -> handleSyncOutcome(syncResult.data)
            }
        }
    }

    private fun disconnectCloudBackup() {
        viewModelScope.launch {
            googleAuthRepository.signOut()
            _state.update { it.copy(lastCloudBackupTime = null, cloudBackupErrorMessage = null) }
        }
    }

    private fun restoreFromCloud() {
        viewModelScope.launch {
            _state.update {
                it.copy(isCloudRestoreInProgress = true, cloudRestoreStatusMessage = null)
            }

            // Contrairement a AppInitializer (demarrage a froid, connexion silencieuse
            // uniquement, echec invisible), ce bouton manuel peut montrer un vrai ecran de
            // connexion si besoin, et rapporte toujours ce qui s'est passe.
            when (val signInResult = googleAuthRepository.signInSilentlyOrPrompt()) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudRestoreInProgress = false,
                            cloudRestoreStatusMessage = signInResult.exception.message,
                        )
                    }
                    return@launch
                }
                is Resource.Success -> Unit
            }

            val hasBackupResult = cloudBackupRepository.hasRemoteBackup()
            when (hasBackupResult) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudRestoreInProgress = false,
                            cloudRestoreStatusMessage = hasBackupResult.exception.message
                                ?: "Impossible de vérifier la sauvegarde cloud",
                        )
                    }
                    return@launch
                }
                is Resource.Success -> if (!hasBackupResult.data) {
                    _state.update {
                        it.copy(
                            isCloudRestoreInProgress = false,
                            cloudRestoreStatusMessage = "Aucune sauvegarde trouvée pour ce compte",
                        )
                    }
                    return@launch
                }
            }

            when (val restoreResult = cloudBackupRepository.restoreAllFromCloud()) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudRestoreInProgress = false,
                            cloudRestoreStatusMessage = restoreResult.exception.message
                                ?: "Échec de la restauration",
                        )
                    }
                }
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isCloudRestoreInProgress = false,
                            cloudRestoreStatusMessage = "Restauration terminée avec succès",
                        )
                    }
                    refreshLastCloudBackupTime()
                }
            }
        }
    }

    private fun openCurrencyCustomiseScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.CurrencyCustomiseScreen)
    }

    private fun showSnapshotPicker() {
        viewModelScope.launch {
            _state.update { it.copy(showSnapshotPicker = true) }
            when (val result = cloudBackupRepository.listSnapshotDates()) {
                is Resource.Success -> _state.update { it.copy(snapshotDates = result.data) }
                is Resource.Error -> _state.update {
                    it.copy(
                        showSnapshotPicker = false,
                        cloudRestoreStatusMessage = result.exception.message
                            ?: "Impossible de charger les sauvegardes datées",
                    )
                }
            }
        }
    }

    private fun restoreFromSnapshotDate(dateKey: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    showSnapshotPicker = false,
                    isCloudRestoreInProgress = true,
                    cloudRestoreStatusMessage = null,
                )
            }
            when (val result = cloudBackupRepository.restoreFromSnapshot(dateKey)) {
                is Resource.Error -> _state.update {
                    it.copy(
                        isCloudRestoreInProgress = false,
                        cloudRestoreStatusMessage = result.exception.message
                            ?: "Échec de la restauration",
                    )
                }
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isCloudRestoreInProgress = false,
                            cloudRestoreStatusMessage = "Restauré depuis la sauvegarde du $dateKey",
                        )
                    }
                    refreshLastCloudBackupTime()
                }
            }
        }
    }

    private fun openExportScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ExportScreen)
    }

    private fun openNotificationScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ReminderScreen)
    }

    private fun openRecurringTransactionsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.RecurringTransactionList)
    }

    private fun openDebtsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.DebtList)
    }

    private fun openSavingsGoalsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.SavingsGoalList)
    }

    private fun openShoppingListsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListList)
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun openAdvancedSettings() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AdvancedSettingsScreen)
    }

    private fun changeDefaultAccount(account: Account) {
        viewModelScope.launch {
            when (val response = settingsRepository.setDefaultAccount(account.id)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    if (response.data) {
                        _state.update { it.copy(selectedAccount = account) }
                    }
                }
            }
        }
    }

    private fun changeSelectedExpenseCategory(category: Category) {
        viewModelScope.launch {
            when (val response = settingsRepository.setDefaultExpenseCategory(category.id)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    if (response.data) {
                        _state.update { it.copy(selectedExpenseCategory = category) }
                    }
                }
            }
        }
    }

    private fun changeSelectedIncomeCategory(category: Category) {
        viewModelScope.launch {
            when (val response = settingsRepository.setDefaultIncomeCategory(category.id)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    if (response.data) {
                        _state.update { it.copy(selectedIncomeCategory = category) }
                    }
                }
            }
        }
    }

    fun processAction(action: SettingAction) {
        when (action) {
            SettingAction.ClosePage -> closePage()
            SettingAction.OpenAdvancedSettings -> openAdvancedSettings()
            SettingAction.OpenCurrencyEdit -> openCurrencyCustomiseScreen()
            SettingAction.OpenExport -> openExportScreen()
            SettingAction.OpenNotification -> openNotificationScreen()
            SettingAction.OpenRecurringTransactions -> openRecurringTransactionsScreen()
            SettingAction.OpenDebts -> openDebtsScreen()
            SettingAction.OpenSavingsGoals -> openSavingsGoalsScreen()
            SettingAction.OpenShoppingLists -> openShoppingListsScreen()
            SettingAction.OpenRateUs -> {
                viewModelScope.launch {
                    _event.send(SettingEvent.RateUs)
                }
            }

            SettingAction.DismissThemeSelection -> {
                _state.update { it.copy(showThemeSelection = false) }
            }

            SettingAction.ShowThemeSelection -> {
                _state.update { it.copy(showThemeSelection = true) }
            }

            SettingAction.DismissLanguageSelection -> {
                _state.update { it.copy(showLanguageSelection = false) }
            }

            SettingAction.ShowLanguageSelection -> {
                _state.update { it.copy(showLanguageSelection = true) }
            }

            is SettingAction.SelectAccount -> changeDefaultAccount(action.account)

            is SettingAction.SelectExpenseCategory -> changeSelectedExpenseCategory(action.category)

            is SettingAction.SelectIncomeCategory -> changeSelectedIncomeCategory(action.category)

            SettingAction.ToggleCompactSummary -> {
                viewModelScope.launch {
                    val newValue = !_state.value.isCompactSummary
                    settingsRepository.setHomeSummaryCompact(newValue)
                    _state.update { it.copy(isCompactSummary = newValue) }
                }
            }

            SettingAction.ToggleAppLock -> {
                viewModelScope.launch {
                    val newValue = !_state.value.isAppLockEnabled
                    settingsRepository.setAppLockEnabled(newValue)
                    _state.update { it.copy(isAppLockEnabled = newValue) }
                }
            }

            SettingAction.Backup -> {
                viewModelScope.launch {
                    _event.send(SettingEvent.Backup)
                }
            }

            SettingAction.Restore -> {
                viewModelScope.launch {
                    _event.send(SettingEvent.Restore)
                }
            }

            SettingAction.ConnectCloudBackup -> connectCloudBackup()

            SettingAction.DisconnectCloudBackup -> disconnectCloudBackup()

            SettingAction.RestoreFromCloud -> restoreFromCloud()

            SettingAction.ShowSnapshotPicker -> showSnapshotPicker()
            SettingAction.DismissSnapshotPicker -> _state.update { it.copy(showSnapshotPicker = false) }
            is SettingAction.RestoreFromSnapshot -> restoreFromSnapshotDate(action.dateKey)

            SettingAction.UseCloudData -> resolveCloudSyncConflict(CloudSyncResolution.USE_CLOUD)

            SettingAction.KeepPhoneData -> resolveCloudSyncConflict(CloudSyncResolution.KEEP_LOCAL)

            is SettingAction.ChangeGeminiApiKey -> {
                viewModelScope.launch {
                    settingsRepository.setGeminiApiKey(action.key)
                }
            }
        }
    }

    private suspend fun handleSyncOutcome(outcome: CloudSyncOutcome) {
        when (outcome) {
            CloudSyncOutcome.Conflict -> {
                _state.update {
                    it.copy(
                        isCloudSyncInProgress = false,
                        showCloudSyncConflict = true,
                    )
                }
            }
            CloudSyncOutcome.Restored -> {
                _state.update { it.copy(isCloudSyncInProgress = false) }
                refreshLastCloudBackupTime()
            }
            CloudSyncOutcome.NoOp -> {
                _state.update { it.copy(isCloudSyncInProgress = false) }
                refreshLastCloudBackupTime()
            }
            CloudSyncOutcome.Pushed -> {
                when (val snapshotResult = cloudBackupRepository.createDailySnapshotIfNeeded()) {
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isCloudSyncInProgress = false,
                                cloudBackupErrorMessage = snapshotResult.exception.message
                                    ?: "La sauvegarde cloud est synchronisée, mais le snapshot n'a pas pu être créé",
                            )
                        }
                    }
                    is Resource.Success -> {
                        _state.update { it.copy(isCloudSyncInProgress = false) }
                    }
                }
                refreshLastCloudBackupTime()
            }
        }
    }

    private fun resolveCloudSyncConflict(resolution: CloudSyncResolution) {
        viewModelScope.launch {
            if (_state.value.isCloudConflictResolving) return@launch
            _state.update { it.copy(isCloudConflictResolving = true, cloudBackupErrorMessage = null) }
            when (val result = cloudBackupRepository.resolveSyncConflict(resolution)) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudConflictResolving = false,
                            cloudBackupErrorMessage = result.exception.message,
                        )
                    }
                }
                is Resource.Success -> {
                    if (result.data == CloudSyncOutcome.Pushed) {
                        cloudBackupRepository.createDailySnapshotIfNeeded()
                    }
                    _state.update {
                        it.copy(
                            isCloudConflictResolving = false,
                            showCloudSyncConflict = false,
                        )
                    }
                    refreshLastCloudBackupTime()
                }
            }
        }
    }
}
