package com.naveenapps.expensemanager.feature.worktime.calculator

import com.naveenapps.expensemanager.core.model.WorkTimeUnit

sealed class WorkTimeCalculatorAction {
    data class SetPrice(val value: String) : WorkTimeCalculatorAction()
    data class SetUnit(val unit: WorkTimeUnit) : WorkTimeCalculatorAction()
    data object Calculate : WorkTimeCalculatorAction()
    data object OpenSettings : WorkTimeCalculatorAction()
    data class ToggleAutoDetect(val enabled: Boolean) : WorkTimeCalculatorAction()
}

