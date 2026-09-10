package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import java.util.Date
import kotlinx.coroutines.flow.Flow

interface PendingTransactionRepository {

    fun getAllPendingTransactions(): Flow<List<PendingTransaction>>

    fun getPendingTransactionById(id: String): Flow<PendingTransaction?>

    suspend fun addPendingTransaction(transaction: PendingTransaction): Resource<Boolean>

    suspend fun deletePendingTransaction(id: String): Resource<Boolean>

    suspend fun deleteAllPendingTransactions(): Resource<Boolean>

    suspend fun updateScheduledDate(id: String, newDate: Date): Resource<Boolean>

    suspend fun countScheduledDue(): Int
}

