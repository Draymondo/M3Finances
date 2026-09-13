package com.naveenapps.expensemanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.naveenapps.expensemanager.core.model.WorkTimePeriod
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistance des réglages de l'outil "Temps de travail équivalent" dans le DataStore
 * partagé de l'application (`expense_manager_app_data_store`).
 *
 * Un seul objet de réglages — pas de liste, pas de Room.
 */
class WorkTimeSettingsDataStore(private val dataStore: DataStore<Preferences>) {

    fun getSettings(): Flow<WorkTimeSettings> = dataStore.data.map { prefs ->
        val categoryIdsRaw = prefs[KEY_AUTO_DETECT_CATEGORY_IDS] ?: ""
        val categoryIds = if (categoryIdsRaw.isBlank()) emptyList()
        else categoryIdsRaw.split(CATEGORY_DELIMITER).filter { it.isNotBlank() }

        WorkTimeSettings(
            manualIncome = prefs[KEY_MANUAL_INCOME] ?: 0.0,
            manualIncomePeriod = WorkTimePeriod.entries.getOrElse(
                prefs[KEY_MANUAL_INCOME_PERIOD] ?: 0
            ) { WorkTimePeriod.MONTH },
            hoursPerDay = prefs[KEY_HOURS_PER_DAY] ?: 8.0,
            daysPerWeek = prefs[KEY_DAYS_PER_WEEK] ?: 5,
            autoDetectEnabled = prefs[KEY_AUTO_DETECT_ENABLED] ?: false,
            autoDetectCategoryIds = categoryIds,
        )
    }

    suspend fun updateSettings(settings: WorkTimeSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_MANUAL_INCOME] = settings.manualIncome
            prefs[KEY_MANUAL_INCOME_PERIOD] = settings.manualIncomePeriod.ordinal
            prefs[KEY_HOURS_PER_DAY] = settings.hoursPerDay
            prefs[KEY_DAYS_PER_WEEK] = settings.daysPerWeek
            prefs[KEY_AUTO_DETECT_ENABLED] = settings.autoDetectEnabled
            prefs[KEY_AUTO_DETECT_CATEGORY_IDS] =
                settings.autoDetectCategoryIds.joinToString(CATEGORY_DELIMITER)
        }
    }

    suspend fun setAutoDetectEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_DETECT_ENABLED] = enabled
        }
    }

    companion object {
        private val KEY_MANUAL_INCOME = doublePreferencesKey("work_time_manual_income")
        private val KEY_MANUAL_INCOME_PERIOD = intPreferencesKey("work_time_manual_income_period")
        private val KEY_HOURS_PER_DAY = doublePreferencesKey("work_time_hours_per_day")
        private val KEY_DAYS_PER_WEEK = intPreferencesKey("work_time_days_per_week")
        private val KEY_AUTO_DETECT_ENABLED = booleanPreferencesKey("work_time_auto_detect_enabled")
        private val KEY_AUTO_DETECT_CATEGORY_IDS = stringPreferencesKey("work_time_auto_detect_category_ids")
        private const val CATEGORY_DELIMITER = ","
    }
}

