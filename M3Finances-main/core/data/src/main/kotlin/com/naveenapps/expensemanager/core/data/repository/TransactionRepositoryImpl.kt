package com.naveenapps.expensemanager.core.data.repository

import android.util.Log
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.fromLocalToUTCTimeStamp
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.CategoryDao
import com.naveenapps.expensemanager.core.database.dao.TransactionDao
import com.naveenapps.expensemanager.core.database.entity.TransactionEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionRelation
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.TransactionSplitItem
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.isTransfer
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.Normalizer

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val dispatchers: AppCoroutineDispatchers,
) : TransactionRepository {

    override fun getAllTransaction(): Flow<List<Transaction>?> =
        transactionDao.getAllTransaction().map {
            convertTransactionAndCategory(it)
        }

    override fun searchTransactions(query: String): Flow<List<Transaction>?> {
        val normalizedQuery = Normalizer.normalize(query, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
        return transactionDao.searchTransactions(normalizedQuery).map {
            convertTransactionAndCategory(it)
        }
    }

    override suspend fun findTransactionById(transactionId: String): Resource<Transaction> =
        withContext(dispatchers.io) {
            return@withContext try {
                val transaction = transactionDao.findRelationById(transactionId)

                val convertedTransaction = transaction?.let { convertTransactionCategoryRelation(it) }
                if (convertedTransaction != null) {
                    Resource.Success(convertedTransaction)
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }

    override suspend fun getTransactionsByAccountId(accountId: String): List<Transaction> =
        withContext(dispatchers.io) {
            return@withContext transactionDao.getTransactionsByAccountId(accountId).map { it.toDomainModel() }
        }

    override suspend fun addTransaction(transaction: Transaction): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = transactionDao.insertTransaction(
                    transaction.toEntityModel(),
                    transaction.splitItems.map { it.toEntityModel() },
                    if (transaction.type == TransactionType.INCOME) {
                        transaction.amount.amount
                    } else {
                        transaction.amount.amount * -1
                    },
                    transaction.type.isTransfer(),
                )
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun updateTransaction(transaction: Transaction): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val transactionEntity = transaction.toEntityModel()
                transactionDao.removePreviousEnteredAmount(transactionEntity)
                transactionDao.updateTransaction(
                    transactionEntity,
                    transaction.splitItems.map { it.toEntityModel() },
                    if (transaction.type.isIncome()) {
                        transaction.amount.amount
                    } else {
                        transaction.amount.amount * -1
                    },
                    transaction.type.isTransfer(),
                )
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun deleteTransaction(transaction: Transaction): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val transactionEntity = transaction.toEntityModel()
                transactionDao.removePreviousEnteredAmount(transactionEntity)
                val response = transactionDao.delete(transactionEntity)
                Resource.Success(response != -1)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    private fun convertTransactionAndCategory(
        transactionWithCategory: List<TransactionRelation>?,
    ): MutableList<Transaction> {
        val outputTransactions = mutableListOf<Transaction>()

        if (transactionWithCategory?.isNotEmpty() == true) {
            transactionWithCategory.forEach {
                convertTransactionCategoryRelation(it)?.let { transaction ->
                    outputTransactions.add(transaction)
                }
            }
        }
        return outputTransactions
    }

    private fun convertTransactionCategoryRelation(relation: TransactionRelation): Transaction? {
        val categoryModel = relation.categoryEntity?.toDomainModel()
        if (categoryModel == null) {
            Log.w(
                "TransactionRepository",
                "Dropping transaction ${relation.transactionEntity.id}: category ${relation.transactionEntity.categoryId} not found (likely deleted)",
            )
            return null
        }
        val fromAccountModel = relation.fromAccountEntity?.toDomainModel()
        if (fromAccountModel == null) {
            Log.w(
                "TransactionRepository",
                "Dropping transaction ${relation.transactionEntity.id}: source account ${relation.transactionEntity.fromAccountId} not found (likely deleted)",
            )
            return null
        }
        return relation.transactionEntity.toDomainModel().apply {
            category = categoryModel
            fromAccount = fromAccountModel
            toAccount = relation.toAccountEntity?.toDomainModel()
            splitItems = relation.splitItems.map { it.toDomainModel() }
        }
    }

    override suspend fun getSplitItemsForTransaction(transactionId: String): List<TransactionSplitItem> =
        transactionDao.getSplitItemsForTransaction(transactionId).map { item ->
            TransactionSplitItem(
                id = item.id,
                transactionId = item.transactionId,
                categoryId = item.categoryId,
                amount = com.naveenapps.expensemanager.core.model.Amount(item.amount),
                notes = item.notes,
                category = categoryDao.findById(item.categoryId)?.toDomainModel(),
            )
        }

    private suspend fun convertTransactionCategoryRelation(relation: TransactionEntity): Transaction {
        val transaction = relation.toDomainModel()
        categoryDao.findById(transaction.categoryId)?.let {
            transaction.category = it.toDomainModel()
        }
        accountDao.findById(transaction.fromAccountId)?.let {
            transaction.fromAccount = it.toDomainModel()
        }
        transaction.toAccountId?.let {
            accountDao.findById(it)?.let { accountEntity ->
                transaction.toAccount = accountEntity.toDomainModel()
            }
        }
        return transaction
    }

    override fun getAllFilteredTransaction(
        accounts: List<String>,
        categories: List<String>,
        transactionType: List<Int>,
    ): Flow<List<Transaction>?> {
        return transactionDao.getAllFilteredTransaction(
            accounts,
            categories,
            transactionType,
        ).map {
            convertTransactionAndCategory(it)
        }
    }

    override fun getFilteredTransaction(
        accounts: List<String>,
        categories: List<String>,
        transactionType: List<Int>,
        startDate: Long,
        endDate: Long,
    ): Flow<List<Transaction>?> {
        return transactionDao.getFilteredTransaction(
            accounts,
            categories,
            transactionType,
            startDate.fromLocalToUTCTimeStamp(),
            endDate.fromLocalToUTCTimeStamp(),
        ).map {
            convertTransactionAndCategory(it)
        }
    }

    override suspend fun getMostFrequentCategoryIdForNotes(
        notes: String,
        transactionType: TransactionType,
    ): String? = withContext(dispatchers.io) {
        return@withContext transactionDao.getMostFrequentCategoryIdForNotes(notes, transactionType)
    }
}
