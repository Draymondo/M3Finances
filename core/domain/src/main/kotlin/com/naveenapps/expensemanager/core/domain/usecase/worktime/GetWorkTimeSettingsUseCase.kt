package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import kotlinx.coroutines.flow.Flow

class GetWorkTimeSettingsUseCase(
    private val repository: WorkTimeSettingsRepository,
) {
    operator fun invoke(): Flow<WorkTimeSettings> = repository.getSettings()
}

