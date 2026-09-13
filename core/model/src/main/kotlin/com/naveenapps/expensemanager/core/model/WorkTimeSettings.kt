package com.naveenapps.expensemanager.core.model

/**
 * Réglages uniques de l'outil "Temps de travail équivalent".
 * Stocké via DataStore Preferences (un seul objet, pas une liste).
 *
 * Le revenu manuel et le revenu auto-détecté sont deux sources concurrentes :
 * le calcul utilise l'un OU l'autre selon [autoDetectEnabled], jamais les deux combinés.
 */
data class WorkTimeSettings(
    /** Revenu saisi manuellement (brut, dans la devise de l'app). */
    val manualIncome: Double = 0.0,
    /** Période associée au revenu manuel (heure / jour / semaine / mois). */
    val manualIncomePeriod: WorkTimePeriod = WorkTimePeriod.MONTH,
    /** Heures travaillées par jour. Doit être > 0 pour un calcul valide. */
    val hoursPerDay: Double = 8.0,
    /** Jours travaillés par semaine. Doit être > 0 pour un calcul valide. */
    val daysPerWeek: Int = 5,
    /** Si true, le taux horaire est calculé à partir des transactions INCOME du mois courant. */
    val autoDetectEnabled: Boolean = false,
    /** IDs des catégories de type INCOME prises en compte pour l'auto-détection. */
    val autoDetectCategoryIds: List<String> = emptyList(),
)

