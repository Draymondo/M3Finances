package com.naveenapps.expensemanager.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationProcessorWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val parseWaveNotificationUseCase: ParseWaveNotificationUseCase by inject()
    private val pendingTransactionRepository: PendingTransactionRepository by inject()

    override suspend fun doWork(): Result {
        val text = inputData.getString(KEY_TEXT) ?: return Result.failure()
        val sourceName = inputData.getString(KEY_SOURCE) ?: return Result.failure()
        val source = runCatching { TransactionSource.valueOf(sourceName) }.getOrDefault(TransactionSource.UNKNOWN)

        Log.d("NotificationWorker", "Processing notification from \$source")

        return when (val result = parseWaveNotificationUseCase.invoke(text, source)) {
            is Resource.Success -> {
                val pendingTx = result.data
                // Because PendingTransactionDao uses REPLACE on conflict, if we receive two similar notifications
                // (Push + SMS) they will both be processed and the second one will silently overwrite the first one,
                // eliminating duplicates automatically as long as Gemini extracts the same transaction_id.
                pendingTransactionRepository.addPendingTransaction(pendingTx)
                Log.d("NotificationWorker", "Successfully saved Pending Transaction: \${pendingTx.id}")
                Result.success()
            }
            is Resource.Error -> {
                val errorMessage = result.exception.message ?: ""
                Log.e("NotificationWorker", "Error parsing notification", result.exception)
                
                // If the error is simply because Gemini couldn't find an amount or it's not a transaction,
                // we should fail and NOT retry forever.
                if (errorMessage.contains("Montant introuvable") || errorMessage.contains("Réponse vide")) {
                    Result.failure()
                } else {
                    // Network errors, quota issues, etc., should be retried.
                    Result.retry()
                }
            }
        }
    }

    companion object {
        const val KEY_TEXT = "notification_text"
        const val KEY_SOURCE = "notification_source"
    }
}

