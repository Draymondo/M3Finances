package com.naveenapps.expensemanager.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.naveenapps.expensemanager.core.model.TransactionSource
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Met en file d'attente WorkManager le traitement d'une notification financière capturée.
 *
 * L'identité du travail est stable pour une même livraison Android (clé de notification, date de
 * publication et contenu), mais n'est PAS l'identité de la transaction financière : deux
 * notifications identiques à des moments différents restent deux événements distincts.
 *
 * Cette identité de travail n'est PAS l'identité de la transaction financière : la
 * déduplication métier est appliquée lors de l'insertion Room, via la référence fournisseur
 * et la clé de déduplication.
 */
class NotificationWorkEnqueuer(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
    private val eventIdProvider: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    private fun notificationEventId(
        packageName: String,
        notificationKey: String?,
        postTime: Long,
        fullText: String,
    ): String {
        if (notificationKey.isNullOrBlank()) return eventIdProvider()

        val identity = "$packageName|$notificationKey|$postTime|$fullText"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }


    /**
     * Enfile le traitement de la notification et renvoie l'identifiant de l'événement utilisé
     * (utile pour la journalisation / le suivi).
     */
    fun enqueue(
        packageName: String,
        source: TransactionSource,
        title: String,
        body: String,
        bigText: String,
        postTime: Long,
        notificationKey: String?,
        fullText: String? = null,
    ): String {
        val resolvedFullText = fullText
            ?: NotificationProcessorWorker.assembleNotificationText(title, body, bigText)
        val eventId = notificationEventId(
            packageName = packageName,
            notificationKey = notificationKey,
            postTime = postTime,
            fullText = resolvedFullText,
        )
        val inputData = NotificationProcessorWorker.buildInputData(
            eventId = eventId,
            packageName = packageName,
            source = source,
            title = title,
            body = body,
            bigText = bigText,
            fullText = resolvedFullText,
            receivedAt = clock(),
            postTime = postTime,
            notificationKey = notificationKey,
        )

        val workRequest = OneTimeWorkRequestBuilder<NotificationProcessorWorker>()
            // Pas de contrainte réseau ici : une notification Wave reconnue par le parser local
            // (3H-1) est traitée immédiatement, même hors connexion. Si Gemini est réellement
            // nécessaire et que le réseau est absent, le worker dépose la notification dans la
            // file persistante et la reprise est planifiée sous contrainte `NetworkType.CONNECTED`.
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .addTag(TAG_NOTIFICATION_TRANSACTION)
            .build()

        // Nom unique par événement : le KEEP ne sert plus qu'à éviter qu'un même événement soit
        // enfilé deux fois, il ne peut plus faire disparaître une seconde notification.
        workManager.enqueueUniqueWork(
            NotificationProcessorWorker.uniqueWorkNameFor(eventId),
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
        return eventId
    }

    companion object {
        /** Tag permettant de retrouver tous les travaux de traitement de notifications. */
        const val TAG_NOTIFICATION_TRANSACTION = "notification_transaction"
    }
}
