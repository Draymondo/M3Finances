package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.InstallmentPlanDao
import com.naveenapps.expensemanager.core.database.entity.InstallmentPlanEntity
import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.InstallmentPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InstallmentPlanRepositoryImpl(
    private val installmentPlanDao: InstallmentPlanDao,
    private val accountDao: AccountDao,
    private val dispatchers: AppCoroutineDispatchers,
) : InstallmentPlanRepository {

    override fun getInstallmentPlans(): Flow<List<InstallmentPlan>> =
        installmentPlanDao.getAll().map { entities ->
            entities.orEmpty().map { it.toEnrichedDomainModel() }
        }

    override suspend fun findInstallmentPlanById(id: String): Resource<InstallmentPlan> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = installmentPlanDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toEnrichedDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun addInstallmentPlan(installmentPlan: InstallmentPlan): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = installmentPlanDao.insert(installmentPlan.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun updateInstallmentPlan(installmentPlan: InstallmentPlan): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                installmentPlanDao.update(installmentPlan.toEntityModel())
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    private suspend fun InstallmentPlanEntity.toEnrichedDomainModel(): InstallmentPlan {
        val plan = toDomainModel()
        accountDao.findById(plan.accountId)?.let {
            return plan.copy(account = it.toDomainModel())
        }
        return plan
    }
}
