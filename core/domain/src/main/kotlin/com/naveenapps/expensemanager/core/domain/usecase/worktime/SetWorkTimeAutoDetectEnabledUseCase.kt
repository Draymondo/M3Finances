package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository

/**
 * Met à jour uniquement le booléen d'auto-détection, sans toucher aux autres réglages.
 * Utilisé par le toggle du Dashboard pour ne pas avoir à relire puis réécrire l'objet complet.
 */
class SetWorkTimeAutoDetectEnabledUseCase(
    private val repository: WorkTimeSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setAutoDetectEnabled(enabled)
    }
}

