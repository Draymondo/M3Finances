package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository pour les réglages de l'outil "Temps de travail équivalent".
 * Source unique de vérité — persisté dans DataStore Preferences.
 */
interface WorkTimeSettingsRepository {

    /** Flow des réglages courants. Émet immédiatement la valeur stockée, puis à chaque
     * modification. */
    fun getSettings(): Flow<WorkTimeSettings>

    /** Remplace l'intégralité des réglages. */
    suspend fun updateSettings(settings: WorkTimeSettings)

    /** Met à jour uniquement le booléen d'auto-détection.
     * Exposé séparément pour le toggle rapide du Dashboard, sans avoir à repasser
     * tout l'objet [WorkTimeSettings]. */
    suspend fun setAutoDetectEnabled(enabled: Boolean)
}

