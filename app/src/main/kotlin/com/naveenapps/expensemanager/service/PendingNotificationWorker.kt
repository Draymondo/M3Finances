package com.naveenapps.expensemanager.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.repository.PendingNotificationProcessingRepository
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import com.naveenapps.expensemanager.network.NetworkMonitor
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Reprise des notifications en attente d'un traitement Gemini.
 *
 * Ce travail est enregistré avec la contrainte [androidx.work.NetworkType.CONNECTED] : il ne
 * s'exécute que lorsqu'une connexion est disponible. Il drainne la file persistante
 * **séquentiellement** afin de ne pas déclencher de rafale d'appels Gemini, et réutilise
 * exactement le même traitement que le worker de capture.
 */
class PendingNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val parseWaveNotificationUseCase: ParseWaveNotificationUseCase by inject()
    private val pendingTransactionRepository: PendingTransactionRepository by inject()
    private val pendingNotificationRepository: PendingNotificationProcessingRepository by inject()
    private val networkMonitor: NetworkMonitor by inject()

    private val processor: NotificationTransactionProcessor by lazy {
        NotificationTransactionProcessor(
            parseWaveNotificationUseCase = parseWaveNotificationUseCase,
            pendingTransactionRepository = pendingTransactionRepository,
            pendingNotificationRepository = pendingNotificationRepository,
            networkMonitor = networkMonitor,
        )
    }

    override suspend fun doWork(): Result {
        val queued = pendingNotificationRepository.getPendingNotifications()
        if (queued.isEmpty()) {
            Log.d(TAG, "Aucune notification en attente de traitement")
            return Result.success()
        }

        var retryNeeded = false
        for (entry in queued) {
            if (isStopped) break
            val outcome = try {
                processor.process(
                    NotificationProcessingRequest(
                        eventId = entry.eventId,
                        packageName = entry.packageName,
                        source = entry.source,
                        channel = entry.channel,
                        title = entry.title,
                        fullText = entry.fullText,
                        postTime = entry.postTime,
                        receivedAt = entry.receivedAt,
                        attemptCount = entry.attemptCount,
                    )
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Log.e(TAG, "Erreur inattendue lors de la reprise de ${entry.eventId}", exception)
                NotificationProcessingOutcome.Retry("exception inattendue: ${exception.message}")
            }

            when (outcome) {
                is NotificationProcessingOutcome.Success ->
                    Log.d(TAG, "Notification ${entry.eventId} traitée depuis la file offline")
                // Devrait être impossible ici (le travail a une contrainte réseau), mais si le
                // réseau disparaît en cours de route, l'entrée reste en file pour plus tard.
                is NotificationProcessingOutcome.Queued ->
                    Log.d(TAG, "Notification ${entry.eventId} ${outcome.reason}")
                is NotificationProcessingOutcome.Retry -> {
                    Log.w(TAG, "Nouvelle tentative requise pour ${entry.eventId} (${outcome.reason})")
                    retryNeeded = true
                }
                is NotificationProcessingOutcome.Failure ->
                    Log.e(TAG, "Abandon définitif de ${entry.eventId} (${outcome.reason})")
            }
        }

        // Une seule politique de retry : celle de WorkManager (backoff exponentiel), bornée par
        // le budget d'attempts porté par chaque entrée de file.
        return if (retryNeeded) Result.retry() else Result.success()
    }

    private companion object {
        const val TAG = "PendingNotificationWorker"
    }
}
