package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {

    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    suspend fun findRecurringTransactionById(id: String): Resource<RecurringTransaction>

    suspend fun addRecurringTransaction(recurringTransaction: RecurringTransaction): Resource<Boolean>

    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): Resource<Boolean>

    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction): Resource<Boolean>

    /**
     * Templates whose [RecurringTransaction.nextOccurrenceDate] has arrived (or passed) and are
     * still active — used by `ProcessDueRecurringTransactionsUseCase`, not intended for direct
     * UI use.
     */
    suspend fun getDueRecurringTransactions(): List<RecurringTransaction>

    /** Advances a template's schedule after it has fired. */
    suspend fun advanceToNextOccurrence(id: String, nextOccurrenceDate: java.util.Date): Resource<Boolean>
}
