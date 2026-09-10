package com.naveenapps.expensemanager.feature.settings

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AppLocale
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.Theme
import java.util.Date

@Stable
data class SettingState(
    val currency: Currency,
    val theme: Theme?,
    val locale: AppLocale? = null,
    val showThemeSelection: Boolean,
    val showLanguageSelection: Boolean = false,
    // Defaults section (moved here from AdvancedSettingState so it's reachable from the main
    // Settings screen instead of being buried inside Advanced).
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val expenseCategories: List<Category> = emptyList(),
    val selectedExpenseCategory: Category? = null,
    val incomeCategories: List<Category> = emptyList(),
    val selectedIncomeCategory: Category? = null,
    val isCompactSummary: Boolean = false,
    // Security section
    val isAppLockEnabled: Boolean = false,
    // Cloud backup section — see GoogleAuthRepository/CloudBackupRepository. This is the
    // explicit, user-initiated connection; the app never prompts for this on its own.
    val isCloudBackupConnected: Boolean = false,
    val isCloudSyncInProgress: Boolean = false,
    val lastCloudBackupTime: Date? = null,
    val cloudBackupErrorMessage: String? = null,
    val isCloudRestoreInProgress: Boolean = false,
    val cloudRestoreStatusMessage: String? = null,
    val showSnapshotPicker: Boolean = false,
    val snapshotDates: List<String> = emptyList(),
    val showCloudSyncConflict: Boolean = false,
    val isCloudConflictResolving: Boolean = false,
    val geminiApiKey: String = "",
)