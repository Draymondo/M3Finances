package com.naveenapps.expensemanager.service

import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.NotificationProcessingException
import com.naveenapps.expensemanager.core.model.PendingNotificationProcessing
import com.naveenapps.expensemanager.core.model.ProcessingErrorType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.PendingNotificationProcessingRepository
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import com.naveenapps.expensemanager.network.NetworkMonitor

/** Résultat du traitement d'une notification, indépendant du moteur de travail qui l'exécute. */
sealed interface NotificationProcessingOutcome {
    /** Transaction enregistrée (`Success(true)`) ou déjà connue (`Success(false)`). */
    data object Success : NotificationProcessingOutcome

    /** Besoin de Gemini mais pas de réseau : la notification est conservée en file. */
    data class Queued(val reason: String) : NotificationProcessingOutcome

    /** Erreur temporaire ou inconnue : une nouvelle tentative est possible. */
    data class Retry(val reason: String) : NotificationProcessingOutcome

    /** Erreur permanente ou budget de tentatives épuisé : abandon explicite. */
    data class Failure(val reason: String) : NotificationProcessingOutcome
}

/** Données minimales nécessaires pour rejouer exactement un traitement. */
data class NotificationProcessingRequest(
    val eventId: String,
    val packageName: String,
    val source: TransactionSource,
    val channel: TransactionChannel,
    val title: String,
    val fullText: String,
    val postTime: Long,
    val receivedAt: Long,
    val attemptCount: Int,
)

/**
 * Traitement d'une notification, partagé par le worker de capture et le worker de reprise offline.
 *
 * Cette classe ne connaît ni WorkManager ni Android : elle est directement testable.
 *
 * Règles :
 * - une notification reconnue par le parser local Wave **n'a jamais besoin du réseau** ;
 * - une notification nécessitant Gemini sans réseau est conservée en file persistante ;
 * - les garanties 3D/3F restent respectées : `Success(true)` et `Success(false)` sont tous deux des
 *   succès, et l'entrée de file est alors supprimée.
 */
class NotificationTransactionProcessor(
    private val parseWaveNotificationUseCase: ParseWaveNotificationUseCase,
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val pendingNotificationRepository: PendingNotificationProcessingRepository,
    private val networkMonitor: NetworkMonitor,
    private val maxAttempts: Int = NotificationProcessorWorker.MAX_ATTEMPTS,
) {

    suspend fun process(request: NotificationProcessingRequest): NotificationProcessingOutcome {
        val postTime = request.postTime.takeIf { it > 0L }

        if (parseWaveNotificationUseCase.requiresGemini(request.fullText, request.source, postTime) &&
            !networkMonitor.isNetworkAvailable()
        ) {
            return enqueue(request)
        }

        return when (val parseResult = parseWaveNotificationUseCase.invoke(request.fullText, request.source, postTime)) {
            is Resource.Success -> save(request, parseResult.data)
            is Resource.Error -> handleError(
                request,
                parseResult.exception,
                "erreur de parsing: ${parseResult.exception.message}",
            )
        }
    }

    private suspend fun enqueue(request: NotificationProcessingRequest): NotificationProcessingOutcome =
        when (val queued = pendingNotificationRepository.enqueue(request.toQueueEntry())) {
            is Resource.Success -> NotificationProcessingOutcome.Queued(
                if (queued.data) "mise en file offline" else "déjà en file offline",
            )
            // La file n'a pas pu être écrite : on ne déclare pas un succès mensonger, on retente.
            is Resource.Error -> handleError(
                request,
                queued.exception,
                "échec de mise en file: ${queued.exception.message}",
            )
        }

    private suspend fun save(
        request: NotificationProcessingRequest,
        pendingTransaction: com.naveenapps.expensemanager.core.model.PendingTransaction,
    ): NotificationProcessingOutcome {
        val toSave = pendingTransaction.copy(
            channel = request.channel,
            notificationEventId = request.eventId.takeIf { it.isNotBlank() },
        )
        return when (val addResult = pendingTransactionRepository.addPendingTransaction(toSave)) {
            is Resource.Success -> {
                // `Success(true)` et `Success(false)` sont deux succès fonctionnels : dans les deux
                // cas la notification a été traitée et l'entrée de file peut disparaître.
                pendingNotificationRepository.delete(request.eventId)
                NotificationProcessingOutcome.Success
            }
            is Resource.Error -> handleError(
                request,
                addResult.exception,
                "échec d'enregistrement: ${addResult.exception.message}",
            )
        }
    }

    private suspend fun handleError(
        request: NotificationProcessingRequest,
        exception: Exception,
        reason: String,
    ): NotificationProcessingOutcome {
        val isPermanent = (exception as? NotificationProcessingException)?.type == ProcessingErrorType.PERMANENT ||
            PERMANENT_ERROR_MARKERS.any { marker -> reason.contains(marker) }
        return if (isPermanent) {
            // Erreur définitive : on retire l'entrée pour ne jamais la retraiter indéfiniment.
            pendingNotificationRepository.delete(request.eventId)
            NotificationProcessingOutcome.Failure(reason)
        } else if (request.attemptCount >= maxAttempts - 1) {
            pendingNotificationRepository.delete(request.eventId)
            NotificationProcessingOutcome.Failure(reason)
        } else {
            // Erreur temporaire : l'entrée est conservée pour une nouvelle tentative.
            pendingNotificationRepository.recordFailure(request.eventId, reason)
            NotificationProcessingOutcome.Retry(reason)
        }
    }

    private fun NotificationProcessingRequest.toQueueEntry() = PendingNotificationProcessing(
        eventId = eventId,
        packageName = packageName,
        source = source,
        channel = channel,
        title = title,
        fullText = fullText,
        postTime = postTime,
        receivedAt = receivedAt,
    )

    companion object {
        /** Marqueur d'erreur définitive, strictement aligné sur celui du worker 3G. */
        internal val PERMANENT_ERROR_MARKERS = listOf(
            "Montant introuvable",
            "Montant invalide",
            "Frais invalides",
            "Réponse vide",
            "Réponse JSON Gemini invalide",
            "Clé d'API Gemini non configurée",
            "Type de transaction absent ou invalide",
            "Date de transaction absente ou invalide",
        )
    }
}
