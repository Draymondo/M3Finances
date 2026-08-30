package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionSplitItem
import com.naveenapps.expensemanager.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun findTransactionById(transactionId: String): Resource<Transaction>

    suspend fun addTransaction(transaction: Transaction): Resource<Boolean>

    suspend fun updateTransaction(transaction: Transaction): Resource<Boolean>

    suspend fun deleteTransaction(transaction: Transaction): Resource<Boolean>

    suspend fun getSplitItemsForTransaction(transactionId: String): List<TransactionSplitItem>

    fun getAllTransaction(): Flow<List<Transaction>?>

    fun searchTransactions(query: String): Flow<List<Transaction>?>

    fun getAllFilteredTransaction(
        accounts: List<String>,
        categories: List<String>,
        transactionType: List<Int>,
    ): Flow<List<Transaction>?>

    fun getFilteredTransaction(
        accounts: List<String>,
        categories: List<String>,
        transactionType: List<Int>,
        startDate: Long,
        endDate: Long,
    ): Flow<List<Transaction>?>

    suspend fun getMostFrequentCategoryIdForNotes(notes: String, transactionType: TransactionType): String?
}
