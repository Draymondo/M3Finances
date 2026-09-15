package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.database.dao.PendingTransactionDao
import com.naveenapps.expensemanager.core.database.entity.PendingTransactionEntity
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class PendingTransactionRepositoryImpl(
    private val pendingTransactionDao: PendingTransactionDao
) : PendingTransactionRepository {

    override fun getAllPendingTransactions(): Flow<List<PendingTransaction>> {
        // Uniquement les transactions "actionables maintenant" (SMS capturées, ou programmées
        // déjà dues) — c'est cette liste qui alimente le compteur de notifications de l'accueil.
        return pendingTransactionDao.getActionablePendingTransactions(System.currentTimeMillis()).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllScheduledPendingTransactions(): Flow<List<PendingTransaction>> {
        // Toutes les transactions prévues, à venir comme dues — pour l'écran dédié uniquement.
        return pendingTransactionDao.getAllPendingTransactions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getPendingTransactionById(id: String): Flow<PendingTransaction?> {
        return pendingTransactionDao.getPendingTransactionById(id).map { it?.toDomainModel() }
    }

    override suspend fun addPendingTransaction(transaction: PendingTransaction): Resource<Boolean> {
        return try {
            pendingTransactionDao.insert(transaction.toEntityModel())
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun deletePendingTransaction(id: String): Resource<Boolean> {
        return try {
            pendingTransactionDao.delete(id)
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun deleteAllPendingTransactions(): Resource<Boolean> {
        return try {
            pendingTransactionDao.deleteAll()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun updateScheduledDate(id: String, newDate: Date): Resource<Boolean> {
        return try {
            pendingTransactionDao.updateScheduledDate(id, newDate.time)
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun countScheduledDue(): Int {
        return try {
            pendingTransactionDao.countScheduledDue(System.currentTimeMillis())
        } catch (e: Exception) {
            0
        }
    }

    private fun PendingTransactionEntity.toDomainModel(): PendingTransaction {
        return PendingTransaction(
            id = id,
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = Date(date),
            transactionType = transactionType,
            suggestedCategory = suggestedCategory,
            rawNotification = rawNotification,
            source = source,
            confidence = confidence,
            scheduledDate = scheduledDate?.let { Date(it) },
            accountId = accountId,
            categoryId = categoryId,
        )
    }

    private fun PendingTransaction.toEntityModel(): PendingTransactionEntity {
        return PendingTransactionEntity(
            id = id,
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = date.time,
            transactionType = transactionType,
            suggestedCategory = suggestedCategory,
            rawNotification = rawNotification,
            source = source,
            confidence = confidence,
            createdOn = System.currentTimeMillis(),
            scheduledDate = scheduledDate?.time,
            accountId = accountId,
            categoryId = categoryId,
        )
    }
}

