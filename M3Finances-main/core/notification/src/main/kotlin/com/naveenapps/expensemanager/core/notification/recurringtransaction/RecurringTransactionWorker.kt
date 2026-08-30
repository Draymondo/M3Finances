package com.naveenapps.expensemanager.core.notification.recurringtransaction

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.ProcessDueRecurringTransactionsUseCase

class RecurringTransactionWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            processDueRecurringTransactionsUseCase.invoke()
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}
