package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.datastore.WorkTimeSettingsDataStore
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import kotlinx.coroutines.flow.Flow

class WorkTimeSettingsRepositoryImpl(
    private val workTimeSettingsDataStore: WorkTimeSettingsDataStore,
) : WorkTimeSettingsRepository {

    override fun getSettings(): Flow<WorkTimeSettings> =
        workTimeSettingsDataStore.getSettings()

    override suspend fun updateSettings(settings: WorkTimeSettings) =
        workTimeSettingsDataStore.updateSettings(settings)

    override suspend fun setAutoDetectEnabled(enabled: Boolean) =
        workTimeSettingsDataStore.setAutoDetectEnabled(enabled)
}

