package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.DebtReminderDao
import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DebtReminderRepositoryImpl(
    private val debtReminderDao: DebtReminderDao,
    private val dispatchers: AppCoroutineDispatchers,
) : DebtReminderRepository {

    override fun getRemindersForDebt(debtId: String): Flow<List<DebtReminder>> =
        debtReminderDao.getForDebt(debtId).map { entities ->
            entities.orEmpty().map { it.toDomainModel() }
        }

    override fun getAllReminders(): Flow<List<DebtReminder>> =
        debtReminderDao.getAll().map { entities ->
            entities.orEmpty().map { it.toDomainModel() }
        }

    override suspend fun addReminder(reminder: DebtReminder): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = debtReminderDao.insert(reminder.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun deleteReminder(id: String): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                debtReminderDao.deleteById(id)
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }
}
