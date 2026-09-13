package com.naveenapps.expensemanager.feature.worktime.settings

import com.naveenapps.expensemanager.core.model.WorkTimePeriod

sealed class WorkTimeSettingsAction {
    data class SetIncome(val value: String) : WorkTimeSettingsAction()
    data class SetPeriod(val period: WorkTimePeriod) : WorkTimeSettingsAction()
    data class SetHoursPerDay(val value: String) : WorkTimeSettingsAction()
    data class SetDaysPerWeek(val value: String) : WorkTimeSettingsAction()
    data object ShowCategorySelection : WorkTimeSettingsAction()
    data object DismissCategorySelection : WorkTimeSettingsAction()
    data class SetCategories(val categories: List<com.naveenapps.expensemanager.core.model.Category>) : WorkTimeSettingsAction()
    data object Save : WorkTimeSettingsAction()
    data object ClosePage : WorkTimeSettingsAction()
}

