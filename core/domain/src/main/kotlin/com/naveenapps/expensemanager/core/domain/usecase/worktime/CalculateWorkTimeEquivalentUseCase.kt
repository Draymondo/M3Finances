package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.WorkTimeUnit
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import kotlinx.coroutines.flow.firstOrNull

/** Résultat d'une conversion prix → temps de travail. */
data class WorkTimeResult(
    val value: Double,
    val unit: WorkTimeUnit,
)

/**
 * Convertit un prix en temps de travail équivalent.
 *
 * **Formules (exactement celles de l'AGENTS.md) :**
 * ```
 * heuresTotal = prix / tauxHoraire
 *
 * MINUTE -> heuresTotal * 60
 * HOUR   -> heuresTotal
 * DAY    -> heuresTotal / hoursPerDay
 * MONTH  -> heuresTotal / hoursPerMonth
 * ```
 *
 * Propage toutes les erreurs de [GetEffectiveHourlyRateUseCase] sans les avaler.
 */
class CalculateWorkTimeEquivalentUseCase(
    private val repository: WorkTimeSettingsRepository,
    private val getEffectiveHourlyRateUseCase: GetEffectiveHourlyRateUseCase,
) {
    suspend operator fun invoke(price: Double, unit: WorkTimeUnit): Resource<WorkTimeResult> {
        if (price <= 0.0) {
            return Resource.Error(Exception("Le prix doit être supérieur à 0"))
        }

        return when (val rateResult = getEffectiveHourlyRateUseCase()) {
            is Resource.Error -> rateResult
            is Resource.Success -> {
                val hourlyRate = rateResult.data
                val settings = repository.getSettings().firstOrNull()
                    ?: return Resource.Error(Exception("Impossible de lire les réglages"))

                val hoursPerMonth = 4.33 * settings.daysPerWeek * settings.hoursPerDay
                val totalHours = price / hourlyRate

                val value = when (unit) {
                    WorkTimeUnit.MINUTE -> totalHours * 60.0
                    WorkTimeUnit.HOUR -> totalHours
                    WorkTimeUnit.DAY -> totalHours / settings.hoursPerDay
                    WorkTimeUnit.MONTH -> totalHours / hoursPerMonth
                }
                Resource.Success(WorkTimeResult(value = value, unit = unit))
            }
        }
    }
}

