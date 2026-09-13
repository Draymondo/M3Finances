package com.naveenapps.expensemanager.feature.worktime.settings

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.WorkTimePeriod

@Stable
data class WorkTimeSettingsState(
    val income: TextFieldValue<String>,
    val selectedPeriod: WorkTimePeriod = WorkTimePeriod.MONTH,
    val hoursPerDay: TextFieldValue<String>,
    val daysPerWeek: TextFieldValue<String>,
    /** Catégories de type INCOME disponibles pour la sélection. */
    val availableCategories: List<Category> = emptyList(),
    /** IDs des catégories sélectionnées pour l'auto-détection. */
    val selectedCategoryIds: List<String> = emptyList(),
    val selectedCategories: List<Category> = emptyList(),
    val showCategorySelection: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
)

