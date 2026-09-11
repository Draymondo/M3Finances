package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.EnvelopeDao
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EnvelopeRepositoryImpl(
    private val envelopeDao: EnvelopeDao,
    private val dispatchers: AppCoroutineDispatchers,
) : EnvelopeRepository {

    override fun getEnvelopes(): Flow<List<Envelope>> =
        envelopeDao.getEnvelopes().map { entities ->
            entities.orEmpty().map { it.toDomainModel() }
        }

    override suspend fun findEnvelopeById(id: String): Resource<Envelope> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = envelopeDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun findEnvelopesByCategoryAndPeriod(
        categoryId: String,
        selectedMonth: String,
        periodType: BudgetPeriod,
    ): List<Envelope> = withContext(dispatchers.io) {
        envelopeDao.findByCategoryAndPeriod(
            categoryId = categoryId,
            selectedMonth = selectedMonth,
            periodType = periodType.ordinal,
        ).map { it.toDomainModel() }
    }

    override suspend fun findEnvelopesByPeriod(
        selectedMonth: String,
        periodType: BudgetPeriod,
    ): List<Envelope> = withContext(dispatchers.io) {
        envelopeDao.findAllByPeriod(
            selectedMonth = selectedMonth,
            periodType = periodType.ordinal,
        ).map { it.toDomainModel() }
    }

    override suspend fun addEnvelope(envelope: Envelope): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = envelopeDao.insert(envelope.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun updateEnvelope(envelope: Envelope): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                envelopeDao.update(envelope.toEntityModel())
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun deleteEnvelope(envelope: Envelope): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                envelopeDao.delete(envelope.toEntityModel())
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }
}
