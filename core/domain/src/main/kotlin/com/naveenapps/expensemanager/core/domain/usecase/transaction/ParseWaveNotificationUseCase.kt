package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.NotificationProcessingException
import com.naveenapps.expensemanager.core.model.PendingTransactionDeduplication
import com.naveenapps.expensemanager.core.model.ProcessingErrorType
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
class ParseWaveNotificationUseCase(
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository,
    private val waveNotificationParser: WaveNotificationParser = WaveNotificationParser(),
) {
    suspend fun invoke(
        notificationText: String,
        source: TransactionSource,
        postTime: Long? = null,
    ): Resource<PendingTransaction> {
        val local = parseLocally(notificationText, source, postTime)
        if (local != null) return Resource.Success(local)

        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull().orEmpty()
        if (apiKey.isBlank()) {
            return Resource.Error(
                NotificationProcessingException(
                    ProcessingErrorType.PERMANENT,
                    "Clé d'API Gemini non configurée.",
                )
            )
        }
        return geminiRepository.parseWaveNotification(notificationText, source, apiKey)
    }

    /**
     * Indique si ce texte doit transiter par Gemini.
     *
     * Expose exactement la même règle métier que [invoke] (parser local 3H-1 d'abord, sinon
     * Gemini), afin que l'appelant puisse décider s'il a besoin du réseau **avant** d'appeler le
     * modèle, sans jamais dupliquer la décision.
     */
    fun requiresGemini(notificationText: String, source: TransactionSource, postTime: Long? = null): Boolean =
        parseLocally(notificationText, source, postTime) == null

    /** Résultat du parser local Wave, ou `null` si Gemini est nécessaire. */
    private fun parseLocally(
        notificationText: String,
        source: TransactionSource,
        postTime: Long?,
    ): PendingTransaction? {
        if (source != TransactionSource.WAVE) return null
        return when (val local = waveNotificationParser.parse(notificationText, postTime)) {
            is WaveParseResult.Recognized -> local.transaction.toPendingTransaction(notificationText, source)
            is WaveParseResult.NotRecognized -> null
        }
    }

    private fun ParsedWaveTransaction.toPendingTransaction(
        notificationText: String,
        source: TransactionSource,
    ): PendingTransaction {
        val deduplicationKey = PendingTransactionDeduplication.computeDeduplicationKey(
            source = source,
            type = transactionType,
            amount = amount,
            senderOrReceiver = merchant,
            fee = fee,
        )
        return PendingTransaction(
            id = java.util.UUID.randomUUID().toString(),
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = date,
            transactionType = transactionType,
            suggestedCategory = null,
            rawNotification = notificationText,
            source = source,
            confidence = 1f,
            providerReference = providerReference,
            deduplicationKey = deduplicationKey,
        )
    }
}
