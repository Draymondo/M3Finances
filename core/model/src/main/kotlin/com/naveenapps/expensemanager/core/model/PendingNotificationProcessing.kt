package com.naveenapps.expensemanager.core.model

/**
 * Notification capturée qui attend un traitement nécessitant Gemini.
 *
 * La file est volontairement minimale : elle ne conserve que ce qui est indispensable pour
 * **rejouer exactement** le même traitement que celui qui aurait eu lieu immédiatement.
 *
 * L'identité de l'entrée est l'`eventId` calculé au moment de la capture (chantier 3D) : il n'est
 * jamais recalculé ici. La clé primaire étant cet `eventId`, un rejeu Android de la même
 * notification ne peut pas créer une deuxième entrée.
 *
 * `fullText` est conservé tel quel (contenu enrichi du chantier 3C) : la notification n'est
 * jamais reconstruite à partir de fragments au moment du rejeu.
 */
data class PendingNotificationProcessing(
    val eventId: String,
    val packageName: String,
    val source: TransactionSource,
    val channel: TransactionChannel,
    val title: String,
    val fullText: String,
    val postTime: Long,
    val receivedAt: Long,
    val status: PendingNotificationStatus = PendingNotificationStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

/**
 * États possibles d'une notification en attente.
 *
 * `FAILED` n'est volontairement pas conservé : une erreur permanente supprime l'entrée
 * plutôt que de la faire tourner indéfiniment.
 */
enum class PendingNotificationStatus {
    PENDING,
    PROCESSING,
}
