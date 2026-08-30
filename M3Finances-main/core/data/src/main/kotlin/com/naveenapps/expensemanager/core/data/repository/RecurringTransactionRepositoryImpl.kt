package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.CategoryDao
import com.naveenapps.expensemanager.core.database.dao.RecurringTransactionDao
import com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity
import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date

class RecurringTransactionRepositoryImpl(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val dispatchers: AppCoroutineDispatchers,
) : RecurringTransactionRepository {

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getAll().map { entities ->
            entities.orEmpty().map { it.toDomainModel() }
        }

    override suspend fun findRecurringTransactionById(id: String): Resource<RecurringTransaction> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = recurringTransactionDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toEnrichedDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun addRecurringTransaction(
        recurringTransaction: RecurringTransaction,
    ): Resource<Boolean> = withContext(dispatchers.io) {
        return@withContext try {
            val response = recurringTransactionDao.insert(recurringTransaction.toEntityModel())
            Resource.Success(response != -1L)
        } catch (exception: Exception) {
            Resource.Error(exception)
        }
    }

    override suspend fun updateRecurringTransaction(
        recurringTransaction: RecurringTransaction,
    ): Resource<Boolean> = withContext(dispatchers.io) {
        return@withContext try {
            recurringTransactionDao.update(recurringTransaction.toEntityModel())
            Resource.Success(true)
        } catch (exception: Exception) {
            Resource.Error(exception)
        }
    }

    override suspend fun deleteRecurringTransaction(
        recurringTransaction: RecurringTransaction,
    ): Resource<Boolean> = withContext(dispatchers.io) {
        return@withContext try {
            val response = recurringTransactionDao.delete(recurringTransaction.toEntityModel())
            Resource.Success(response != -1)
        } catch (exception: Exception) {
            Resource.Error(exception)
        }
    }

    override suspend fun getDueRecurringTransactions(): List<RecurringTransaction> =
        withContext(dispatchers.io) {
            recurringTransactionDao.getDueRecurringTransactions(Date())
                .map { it.toEnrichedDomainModel() }
        }

    override suspend fun advanceToNextOccurrence(
        id: String,
        nextOccurrenceDate: Date,
    ): Resource<Boolean> = withContext(dispatchers.io) {
        return@withContext try {
            recurringTransactionDao.updateNextOccurrence(id, nextOccurrenceDate, Date())
            Resource.Success(true)
        } catch (exception: Exception) {
            Resource.Error(exception)
        }
    }

    private suspend fun RecurringTransactionEntity.toEnrichedDomainModel(): RecurringTransaction {
        val recurringTransaction = toDomainModel()
        categoryDao.findById(recurringTransaction.categoryId)?.let {
            recurringTransaction.category = it.toDomainModel()
        }
        accountDao.findById(recurringTransaction.fromAccountId)?.let {
            recurringTransaction.fromAccount = it.toDomainModel()
        }
        recurringTransaction.toAccountId?.let { toAccountId ->
            accountDao.findById(toAccountId)?.let {
                recurringTransaction.toAccount = it.toDomainModel()
            }
        }
        return recurringTransaction
    }
}
