package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.DebtDao
import com.naveenapps.expensemanager.core.database.entity.DebtEntity
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DebtRepositoryImpl(
    private val debtDao: DebtDao,
    private val accountDao: AccountDao,
    private val dispatchers: AppCoroutineDispatchers,
) : DebtRepository {

    override fun getDebts(): Flow<List<Debt>> =
        debtDao.getAll().map { entities ->
            entities.orEmpty().map { it.toEnrichedDomainModel() }
        }

    override suspend fun findDebtById(id: String): Resource<Debt> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = debtDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toEnrichedDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun addDebt(debt: Debt): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = debtDao.insert(debt.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun updateDebt(debt: Debt): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                debtDao.update(debt.toEntityModel())
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    private suspend fun DebtEntity.toEnrichedDomainModel(): Debt {
        val debt = toDomainModel()
        accountDao.findById(debt.accountId)?.let {
            return debt.copy(account = it.toDomainModel())
        }
        return debt
    }
}
