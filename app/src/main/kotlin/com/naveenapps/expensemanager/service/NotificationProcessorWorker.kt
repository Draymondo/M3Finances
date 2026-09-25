package com.naveenapps.expensemanager.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.PendingNotificationProcessingRepository
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import com.naveenapps.expensemanager.network.NetworkMonitor
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationProcessorWorker(
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
        val eventId = inputData.getString(KEY_EVENT_ID).orEmpty()
        val text = inputData.getString(KEY_TEXT)
        val sourceName = inputData.getString(KEY_SOURCE)

        if (text.isNullOrBlank() || sourceName.isNullOrBlank()) {
            // Données incomplètes : une nouvelle tentative n'apporterait rien, on abandonne
            // la notification en le signalant explicitement.
            Log.e(TAG, "Abandon de la notification $eventId : texte ou source manquant.")
            return Result.failure()
        }

        val rawSource = runCatching { TransactionSource.valueOf(sourceName) }
            .getOrDefault(TransactionSource.UNKNOWN)

        val channel = if (rawSource == TransactionSource.SMS) {
            TransactionChannel.SMS
        } else {
            TransactionChannel.PUSH
        }

        val title = inputData.getString(KEY_TITLE).orEmpty()
        val provider = if (rawSource == TransactionSource.SMS) {
            deduceProviderFromTitle(title)
        } else {
            rawSource
        }

        Log.d(
            TAG,
            "Traitement de la notification $eventId " +
                "(provider=$provider, channel=$channel, package=${inputData.getString(KEY_PACKAGE_NAME)}, tentative=$runAttemptCount)"
        )

        val request = NotificationProcessingRequest(
            eventId = eventId,
            packageName = inputData.getString(KEY_PACKAGE_NAME).orEmpty(),
            source = provider,
            channel = channel,
            title = title,
            fullText = text,
            postTime = inputData.getLong(KEY_POST_TIME, 0L),
            receivedAt = inputData.getLong(KEY_RECEIVED_AT, 0L),
            attemptCount = runAttemptCount,
        )

        return try {
            when (val outcome = processor.process(request)) {
                is NotificationProcessingOutcome.Success -> Result.success()
                is NotificationProcessingOutcome.Queued -> {
                    // La notification est en sécurité dans la file persistante et WorkManager
                    // reprendra le traitement dès le retour du réseau : ce n'est pas un échec.
                    Log.i(TAG, "Notification $eventId ${outcome.reason} (traitement différé)")
                    // La planification de la reprise ne doit jamais faire échouer un traitement
                    // déjà réussi : si elle échoue, la notification reste en file et sera reprise
                    // au prochain enfilage ou au prochain démarrage de l'application.
                    runCatching { PendingNotificationWorkScheduler.enqueueResume(applicationContext) }
                        .onFailure { Log.w(TAG, "Reprise non planifiée pour l'instant", it) }
                    Result.success()
                }
                is NotificationProcessingOutcome.Retry -> retryOrFail(eventId, outcome.reason)
                is NotificationProcessingOutcome.Failure -> {
                    Log.e(TAG, "Abandon définitif de la notification $eventId (${outcome.reason})")
                    Result.failure()
                }
            }
        } catch (cancellation: CancellationException) {
            // Travail annulé (WorkManager / arrêt du processus) : ne pas transformer
            // l'annulation en échec métier.
            throw cancellation
        } catch (exception: Exception) {
            // Toute autre exception inattendue ne doit JAMAIS être considérée comme un succès :
            // on retente (dans la limite du budget) au lieu de perdre la notification.
            Log.e(TAG, "Erreur inattendue lors du traitement de la notification $eventId", exception)
            retryOrFail(eventId, "exception inattendue: ${exception.message}")
        }
    }

    /**
     * Programme une nouvelle tentative tant que le budget [MAX_ATTEMPTS] n'est pas épuisé,
     * puis abandonne explicitement : plus de retries infinis.
     */
    private fun retryOrFail(eventId: String, reason: String): Result {
        return if (runAttemptCount >= MAX_ATTEMPTS - 1) {
            Log.e(TAG, "Abandon de la notification $eventId après ${runAttemptCount + 1} tentative(s) ($reason)")
            Result.failure()
        } else {
            Log.w(TAG, "Nouvelle tentative pour la notification $eventId ($reason)")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NotificationWorker"

        /** Nombre total d'exécutions autorisées (première tentative incluse) avant abandon. */
        const val MAX_ATTEMPTS = 5

        /** Clés transmises par [NotificationWorkEnqueuer] (et par l'ancienne version de l'app). */
        const val KEY_TEXT = "notification_text"
        const val KEY_SOURCE = "notification_source"
        const val KEY_EVENT_ID = "notification_event_id"
        const val KEY_PACKAGE_NAME = "notification_package_name"
        const val KEY_TITLE = "notification_title"
        const val KEY_BODY = "notification_body"
        const val KEY_BIG_TEXT = "notification_big_text"
        const val KEY_RECEIVED_AT = "notification_received_at"
        const val KEY_POST_TIME = "notification_post_time"
        const val KEY_NOTIFICATION_KEY = "notification_key"

        /**
         * Nom unique du travail WorkManager pour UN événement de notification.
         *
         * Dérivé de l'identité de livraison Android calculée par l'enqueueur, jamais du seul
         * texte. Un même événement rejoué retrouve le même travail ; deux événements Android
         * différents restent distincts.
         */
        fun uniqueWorkNameFor(eventId: String): String = "notification_tx_$eventId"

        /**
         * Texte transmis au parsing. Les extras Android commonly utilisés par les fournisseurs
         * sont tous conservés afin qu'une notification enrichie ne soit pas rejetée avant Gemini.
         */
        fun assembleNotificationText(
            title: String,
            body: String,
            bigText: String,
            subText: String = "",
            textLines: List<String> = emptyList(),
            messages: List<String> = emptyList(),
        ): String = buildList {
            add(title)
            add(body)
            add(bigText)
            add(subText)
            addAll(textLines)
            addAll(messages)
        }.joinToString("\n").trim()

        /**
         * Construit les données d'entrée du worker en conservant toutes les informations
         * de la notification (source, package, titre, texte, bigText, horodatages, identité).
         */
        fun buildInputData(
            eventId: String,
            packageName: String,
            source: TransactionSource,
            title: String,
            body: String,
            bigText: String,
            fullText: String,
            receivedAt: Long,
            postTime: Long,
            notificationKey: String?,
        ): Data {
            val builder = Data.Builder()
                .putString(KEY_EVENT_ID, eventId)
                .putString(KEY_PACKAGE_NAME, packageName)
                .putString(KEY_SOURCE, source.name)
                .putString(KEY_TITLE, title)
                .putString(KEY_BODY, body)
                .putString(KEY_BIG_TEXT, bigText)
                .putString(KEY_TEXT, fullText)
                .putLong(KEY_RECEIVED_AT, receivedAt)
                .putLong(KEY_POST_TIME, postTime)
            if (notificationKey != null) {
                builder.putString(KEY_NOTIFICATION_KEY, notificationKey)
            }
            return builder.build()
        }

        /**
         * Déduit le fournisseur mobile money d'un SMS à partir du titre / expéditeur uniquement.
         * Aucun parsing du contenu du message. Si inconnu, renvoie UNKNOWN.
         */
        fun deduceProviderFromTitle(title: String): TransactionSource {
            val normalized = title.trim().lowercase(java.util.Locale.ROOT)
            return when {
                "wave" in normalized -> TransactionSource.WAVE
                "orange" in normalized -> TransactionSource.ORANGE_MONEY
                "mtn" in normalized || "momo" in normalized -> TransactionSource.MTN_MOMO
                "moov" in normalized -> TransactionSource.MOOV_MONEY
                "djamo" in normalized -> TransactionSource.DJAMO
                "paypal" in normalized -> TransactionSource.PAYPAL
                else -> TransactionSource.UNKNOWN
            }
        }
    }
}

