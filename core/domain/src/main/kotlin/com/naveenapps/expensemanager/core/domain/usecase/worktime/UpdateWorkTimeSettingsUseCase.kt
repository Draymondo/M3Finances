package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository

/**
 * Valide et persiste les réglages de l'outil "Temps de travail équivalent".
 *
 * Validation effectuée avant écriture :
 * - [WorkTimeSettings.hoursPerDay] doit être > 0
 * - [WorkTimeSettings.daysPerWeek] doit être > 0
 */
class UpdateWorkTimeSettingsUseCase(
    private val repository: WorkTimeSettingsRepository,
) {
    suspend operator fun invoke(settings: WorkTimeSettings): Resource<Boolean> {
        if (settings.hoursPerDay <= 0.0) {
            return Resource.Error(Exception("Le nombre d'heures par jour doit être supérieur à 0"))
        }
        if (settings.daysPerWeek <= 0) {
            return Resource.Error(Exception("Le nombre de jours par semaine doit être supérieur à 0"))
        }
        return try {
            repository.updateSettings(settings)
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }
}

