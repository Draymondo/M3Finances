package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.PendingNotificationProcessing
import com.naveenapps.expensemanager.core.model.PendingNotificationStatus
import com.naveenapps.expensemanager.core.model.Resource

/**
 * File persistante des notifications capturées qui nécessitent un traitement Gemini et qui n'ont
 * pas pu l'être au moment de la capture (typiquement : pas de réseau).
 *
 * Contrat :
 * - [enqueue] est idempotent sur `eventId` : un rejeu Android de la même notification ne crée pas
 *   de deuxième entrée. `Success(false)` signifie « déjà en file ».
 * - [getPendingNotifications] retourne les entrées à traiter, dans un ordre déterministe.
 * - [markStatus] sert à transitionner une entrée vers `PROCESSING` le temps de son traitement.
 * - [recordFailure] conserve l'entrée en `PENDING` en cas d'erreur temporaire (politique 3G).
 * - [delete] est appelé après un traitement réussi **ou** idempotent (`Success(true)` et
 *   `Success(false)`) et après une erreur permanente, pour éviter toute boucle infinie.
 */
interface PendingNotificationProcessingRepository {

    suspend fun enqueue(notification: PendingNotificationProcessing): Resource<Boolean>

    suspend fun getPendingNotifications(): List<PendingNotificationProcessing>

    suspend fun markStatus(
        eventId: String,
        status: PendingNotificationStatus,
    ): Resource<Boolean>

    /** Conserve l'entrée en attente et incrémente son compteur de tentatives. */
    suspend fun recordFailure(eventId: String, error: String?): Resource<Boolean>

    suspend fun delete(eventId: String): Resource<Boolean>
}
