package com.naveenapps.expensemanager.feature.worktime.calculator

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.WorkTimeUnit

@Stable
data class WorkTimeCalculatorState(
    val price: TextFieldValue<String>,
    val selectedUnit: WorkTimeUnit = WorkTimeUnit.HOUR,
    /** Résultat formaté, ex: "2.5" (toujours accompagné de [selectedUnit] pour l'affichage). */
    val result: Double? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    /** True si les réglages (hoursPerDay, income…) ne sont pas encore configurés. */
    val isSettingsConfigured: Boolean = false,
    val autoDetectEnabled: Boolean = false,
)

