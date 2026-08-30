package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.SavingsGoalDao
import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SavingsGoalRepositoryImpl(
    private val savingsGoalDao: SavingsGoalDao,
    private val accountDao: AccountDao,
    private val dispatchers: AppCoroutineDispatchers,
) : SavingsGoalRepository {

    override fun getSavingsGoals(): Flow<List<SavingsGoal>> =
        savingsGoalDao.getAll().map { entities ->
            entities.orEmpty().map { it.toEnrichedDomainModel() }
        }

    override suspend fun findSavingsGoalById(id: String): Resource<SavingsGoal> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = savingsGoalDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toEnrichedDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun addSavingsGoal(savingsGoal: SavingsGoal): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = savingsGoalDao.insert(savingsGoal.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun updateSavingsGoal(savingsGoal: SavingsGoal): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                savingsGoalDao.update(savingsGoal.toEntityModel())
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    private suspend fun SavingsGoalEntity.toEnrichedDomainModel(): SavingsGoal {
        val savingsGoal = toDomainModel()
        accountDao.findById(savingsGoal.accountId)?.let {
            return savingsGoal.copy(account = it.toDomainModel())
        }
        return savingsGoal
    }
}
