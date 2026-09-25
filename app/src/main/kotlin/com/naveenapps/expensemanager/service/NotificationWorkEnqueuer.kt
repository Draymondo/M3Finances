package com.naveenapps.expensemanager.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.naveenapps.expensemanager.core.model.TransactionSource
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Met en file d'attente WorkManager le traitement d'une notification financière capturée.
 *
 * L'identité du travail est propre à CHAQUE événement de notification (UUID généré ici) et n'est
 * jamais dérivée du texte : deux notifications au contenu identique correspondent à deux
 * événements distincts et doivent pouvoir être traitées séparément.
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
    ): String {
        val eventId = eventIdProvider()
        val inputData = NotificationProcessorWorker.buildInputData(
            eventId = eventId,
            packageName = packageName,
            source = source,
            title = title,
            body = body,
            bigText = bigText,
            fullText = NotificationProcessorWorker.assembleNotificationText(title, body, bigText),
            receivedAt = clock(),
            postTime = postTime,
            notificationKey = notificationKey,
        )

        val workRequest = OneTimeWorkRequestBuilder<NotificationProcessorWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
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
