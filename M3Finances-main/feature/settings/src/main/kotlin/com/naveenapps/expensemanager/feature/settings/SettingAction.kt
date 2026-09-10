package com.naveenapps.expensemanager.feature.settings

import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.Category

sealed class SettingAction {

    data object ClosePage : SettingAction()

    data object OpenExport : SettingAction()

    data object OpenRateUs : SettingAction()

    data object OpenAdvancedSettings : SettingAction()


    data object OpenNotification : SettingAction()

    data object OpenTools : SettingAction()

    data object OpenCurrencyEdit : SettingAction()

    data object ShowThemeSelection : SettingAction()

    data object DismissThemeSelection : SettingAction()

    data object ShowLanguageSelection : SettingAction()

    data object DismissLanguageSelection : SettingAction()

    // Defaults section (moved here from AdvancedSettingAction)
    data class SelectAccount(val account: Account) : SettingAction()

    data class SelectExpenseCategory(val category: Category) : SettingAction()

    data class SelectIncomeCategory(val category: Category) : SettingAction()

    data object ToggleCompactSummary : SettingAction()

    // Data & Backup section
    data object Backup : SettingAction()

    data object Restore : SettingAction()

    // Security section
    data object ToggleAppLock : SettingAction()

    // Cloud backup section
    data object ConnectCloudBackup : SettingAction()

    data object DisconnectCloudBackup : SettingAction()

    data object RestoreFromCloud : SettingAction()

    data object ShowSnapshotPicker : SettingAction()
    data object DismissSnapshotPicker : SettingAction()
    data class RestoreFromSnapshot(val dateKey: String) : SettingAction()

    data object UseCloudData : SettingAction()

    data object KeepPhoneData : SettingAction()

    data class ChangeGeminiApiKey(val key: String) : SettingAction()
}
