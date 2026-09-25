package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.database.dao.PendingNotificationProcessingDao
import com.naveenapps.expensemanager.core.database.entity.toModel
import com.naveenapps.expensemanager.core.database.entity.toEntityModel
import com.naveenapps.expensemanager.core.model.PendingNotificationProcessing
import com.naveenapps.expensemanager.core.model.PendingNotificationStatus
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.PendingNotificationProcessingRepository

class PendingNotificationProcessingRepositoryImpl(
    private val dao: PendingNotificationProcessingDao,
) : PendingNotificationProcessingRepository {

    override suspend fun enqueue(notification: PendingNotificationProcessing): Resource<Boolean> =
        try {
            // `insert` renvoie -1 quand l'eventId est déjà connu : la notification n'est pas
            // dupliquée, et l'entrée existante n'est jamais écrasée.
            Resource.Success(dao.insert(notification.toEntityModel()) != -1L)
        } catch (e: Exception) {
            Resource.Error(e)
        }

    override suspend fun getPendingNotifications(): List<PendingNotificationProcessing> = try {
        dao.getByStatus(PendingNotificationStatus.PENDING).map { it.toModel() }
    } catch (e: Exception) {
        emptyList()
    }

    override suspend fun markStatus(
        eventId: String,
        status: PendingNotificationStatus,
    ): Resource<Boolean> = try {
        dao.updateStatus(eventId, status)
        Resource.Success(true)
    } catch (e: Exception) {
        Resource.Error(e)
    }

    override suspend fun recordFailure(eventId: String, error: String?): Resource<Boolean> = try {
        dao.recordFailure(eventId, PendingNotificationStatus.PENDING, error)
        Resource.Success(true)
    } catch (e: Exception) {
        Resource.Error(e)
    }

    override suspend fun delete(eventId: String): Resource<Boolean> = try {
        dao.delete(eventId)
        Resource.Success(true)
    } catch (e: Exception) {
        Resource.Error(e)
    }
}
