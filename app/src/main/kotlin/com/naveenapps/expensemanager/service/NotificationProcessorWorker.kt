package com.naveenapps.expensemanager.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationProcessorWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val parseWaveNotificationUseCase: ParseWaveNotificationUseCase by inject()
    private val pendingTransactionRepository: PendingTransactionRepository by inject()

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

        return try {
            when (val parseResult = parseWaveNotificationUseCase.invoke(text, provider)) {
                is Resource.Success -> {
                    val transactionToSave = parseResult.data.copy(
                        channel = channel,
                        notificationEventId = eventId.takeIf { it.isNotBlank() },
                    )
                    savePendingTransaction(eventId, transactionToSave)
                }
                is Resource.Error -> handleParseError(eventId, parseResult.exception)
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

    private suspend fun savePendingTransaction(
        eventId: String,
        pendingTransaction: PendingTransaction,
    ): Result {
        // Room ignore une notification déjà connue via sa référence fournisseur ou sa clé de
        // déduplication : la première ligne, son horodatage et les choix de l'utilisateur sont
        // ainsi conservés lorsqu'une notification est réémise.
        return when (val addResult = pendingTransactionRepository.addPendingTransaction(pendingTransaction)) {
            is Resource.Success -> {
                Log.d(TAG, "Notification $eventId enregistrée (pendingTransaction=${pendingTransaction.id})")
                Result.success()
            }
            is Resource.Error -> {
                Log.e(TAG, "Échec d'enregistrement de la notification $eventId", addResult.exception)
                retryOrFail(eventId, "échec d'enregistrement: ${addResult.exception.message}")
            }
        }
    }

    private fun handleParseError(eventId: String, exception: Exception): Result {
        val message = exception.message.orEmpty()
        Log.e(TAG, "Erreur de parsing de la notification $eventId : $message", exception)

        // Ces erreurs sont définitives : la notification ne contient pas de transaction
        // exploitable, réessayer ne changera rien.
        val isPermanent = PERMANENT_ERROR_MARKERS.any { marker -> message.contains(marker) }
        return if (isPermanent) {
            Result.failure()
        } else {
            retryOrFail(eventId, "erreur de parsing: $message")
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

        private val PERMANENT_ERROR_MARKERS = listOf("Montant introuvable", "Réponse vide")

        /**
         * Nom unique du travail WorkManager pour UN événement de notification.
         *
         * Toujours dérivé de l'identifiant unique de l'événement (UUID généré à la réception),
         * jamais du texte : deux notifications au contenu identique doivent pouvoir être
         * traitées séparément.
         */
        fun uniqueWorkNameFor(eventId: String): String = "notification_tx_$eventId"

        /**
         * Texte transmis au parsing. Reproduit exactement l'ancien format `"$title\n$text\n$bigText"`
         * afin de ne rien changer au comportement des fournisseurs supportés.
         */
        fun assembleNotificationText(title: String, body: String, bigText: String): String =
            "$title\n$body\n$bigText".trim()

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

