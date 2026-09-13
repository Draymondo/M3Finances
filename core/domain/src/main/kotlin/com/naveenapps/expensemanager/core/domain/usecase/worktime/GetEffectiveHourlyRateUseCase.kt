package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.WorkTimePeriod
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Calcule le taux horaire effectif selon les réglages courants.
 *
 * **Ordre exact des vérifications (toutes retournent [Resource.Error] avec message explicite) :**
 * 1. `hoursPerDay <= 0` → erreur "Heures par jour invalides"
 * 2. `daysPerWeek <= 0` → erreur "Jours par semaine invalides"
 * 3. `autoDetectEnabled == false` et `manualIncome <= 0` → erreur "Revenu nul"
 * 4. `autoDetectEnabled == true` et `autoDetectCategoryIds.isEmpty()` → erreur "Aucune catégorie"
 *    (vérification AVANT d'appeler [GetAutoDetectedIncomeUseCase] pour un message précis)
 * 5. `autoDetectEnabled == true` et `revenuAutoDuMois == 0.0` → erreur "Aucun revenu détecté"
 * 6. `tauxHoraire <= 0` (fallback) → erreur "Revenu nul"
 *
 * **Formules (exactement celles de l'AGENTS.md) :**
 * ```
 * heuresParSemaine = daysPerWeek * hoursPerDay
 * heuresParMois    = 4.33 * daysPerWeek * hoursPerDay
 *
 * HOUR   -> tauxHoraire = manualIncome
 * DAY    -> tauxHoraire = manualIncome / hoursPerDay
 * WEEK   -> tauxHoraire = manualIncome / heuresParSemaine
 * MONTH  -> tauxHoraire = manualIncome / heuresParMois
 * ```
 */
class GetEffectiveHourlyRateUseCase(
    private val repository: WorkTimeSettingsRepository,
    private val getAutoDetectedIncomeUseCase: GetAutoDetectedIncomeUseCase,
) {
    suspend operator fun invoke(): Resource<Double> {
        val settings = repository.getSettings().firstOrNull()
            ?: return Resource.Error(Exception("Impossible de lire les réglages"))

        // 1. Vérifications communes (indépendantes du mode)
        if (settings.hoursPerDay <= 0.0) {
            return Resource.Error(Exception("Heures par jour invalides — vérifiez les réglages"))
        }
        if (settings.daysPerWeek <= 0) {
            return Resource.Error(Exception("Jours par semaine invalides — vérifiez les réglages"))
        }

        val hoursPerWeek = settings.daysPerWeek * settings.hoursPerDay
        val hoursPerMonth = 4.33 * settings.daysPerWeek * settings.hoursPerDay

        return if (settings.autoDetectEnabled) {
            // 4. Vérification des catégories AVANT d'appeler le use case auto
            if (settings.autoDetectCategoryIds.isEmpty()) {
                return Resource.Error(
                    Exception("Sélectionnez au moins une catégorie de revenu dans les réglages de l'outil")
                )
            }
            when (val incomeResult = getAutoDetectedIncomeUseCase(settings.autoDetectCategoryIds)) {
                is Resource.Error -> incomeResult
                is Resource.Success -> {
                    val income = incomeResult.data
                    // 5. Aucun revenu ce mois-ci
                    if (income == 0.0) {
                        return Resource.Error(
                            Exception("Aucun revenu détecté ce mois-ci — vérifiez les catégories sélectionnées")
                        )
                    }
                    val rate = income / hoursPerMonth
                    // 6. Fallback
                    if (rate <= 0.0) Resource.Error(Exception("Revenu nul — complétez les réglages"))
                    else Resource.Success(rate)
                }
            }
        } else {
            // 3. Mode manuel : revenu saisi
            if (settings.manualIncome <= 0.0) {
                return Resource.Error(Exception("Revenu nul — complétez les réglages"))
            }
            val rate = when (settings.manualIncomePeriod) {
                WorkTimePeriod.HOUR -> settings.manualIncome
                WorkTimePeriod.DAY -> settings.manualIncome / settings.hoursPerDay
                WorkTimePeriod.WEEK -> settings.manualIncome / hoursPerWeek
                WorkTimePeriod.MONTH -> settings.manualIncome / hoursPerMonth
            }
            // 6. Fallback
            if (rate <= 0.0) Resource.Error(Exception("Revenu nul — complétez les réglages"))
            else Resource.Success(rate)
        }
    }
}

