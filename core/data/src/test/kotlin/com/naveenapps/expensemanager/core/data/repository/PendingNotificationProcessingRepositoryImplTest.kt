package com.naveenapps.expensemanager.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.database.dao.PendingNotificationProcessingDao
import com.naveenapps.expensemanager.core.database.entity.PendingNotificationProcessingEntity
import com.naveenapps.expensemanager.core.model.PendingNotificationProcessing
import com.naveenapps.expensemanager.core.model.PendingNotificationStatus
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests de la file offline (chantier 3I) : déduplication par `eventId` (identité 3D), tri
 * déterministe et conversion des erreurs Room en [Resource.Error].
 */
class PendingNotificationProcessingRepositoryImplTest {

    private val dao: PendingNotificationProcessingDao = mock()
    private val repository = PendingNotificationProcessingRepositoryImpl(dao)

    @Test
    fun `first enqueue reports a new entry`() = runTest {
        whenever(dao.insert(any())).thenReturn(1L)

        val result = repository.enqueue(entry(EVENT_ID))

        assertThat(result).isEqualTo(Resource.Success(true))
    }

    @Test
    fun `replaying the same eventId does not create a second entry`() = runTest {
        // `INSERT IGNORE` renvoie -1 : l'eventId est déjà connu.
        whenever(dao.insert(any())).thenReturn(-1L)

        val result = repository.enqueue(entry(EVENT_ID))

        assertThat(result).isEqualTo(Resource.Success(false))
        verify(dao, times(1)).insert(any())
    }

    @Test
    fun `only pending entries are returned in a deterministic order`() = runTest {
        whenever(dao.getByStatus(PendingNotificationStatus.PENDING))
            .thenReturn(listOf(entity(EVENT_ID), entity("event-2")))

        val pending = repository.getPendingNotifications()

        assertThat(pending.map { it.eventId }).containsExactly(EVENT_ID, "event-2").inOrder()
    }

    @Test
    fun `recordFailure keeps the entry pending`() = runTest {
        whenever(dao.recordFailure(any(), any(), any())).thenReturn(Unit)

        val result = repository.recordFailure(EVENT_ID, "timeout")

        assertThat(result).isEqualTo(Resource.Success(true))
        verify(dao).recordFailure(EVENT_ID, PendingNotificationStatus.PENDING, "timeout")
    }

    @Test
    fun `DAO exception is returned as Resource_Error`() = runTest {
        whenever(dao.insert(any())).thenThrow(IllegalStateException("database is locked"))

        val result = repository.enqueue(entry(EVENT_ID))

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun `reading the queue never throws`() = runTest {
        whenever(dao.getByStatus(PendingNotificationStatus.PENDING))
            .thenThrow(IllegalStateException("database is locked"))

        assertThat(repository.getPendingNotifications()).isEmpty()
    }

    @Test
    fun `delete removes the entry`() = runTest {
        whenever(dao.delete(any())).thenReturn(Unit)

        assertThat(repository.delete(EVENT_ID)).isEqualTo(Resource.Success(true))
        verify(dao).delete(EVENT_ID)
    }

    @Test
    fun `markStatus delegates the transition`() = runTest {
        whenever(dao.updateStatus(any(), any())).thenReturn(Unit)

        val result = repository.markStatus(EVENT_ID, PendingNotificationStatus.PROCESSING)

        assertThat(result).isEqualTo(Resource.Success(true))
        verify(dao).updateStatus(EVENT_ID, PendingNotificationStatus.PROCESSING)
    }

    @Test
    fun `processing state is never returned as actionable`() = runTest {
        whenever(dao.getByStatus(PendingNotificationStatus.PENDING)).thenReturn(emptyList())
        whenever(dao.getByStatus(PendingNotificationStatus.PROCESSING))
            .thenReturn(listOf(entity(EVENT_ID)))

        assertThat(repository.getPendingNotifications()).isEmpty()
        verify(dao, never()).getByStatus(PendingNotificationStatus.PROCESSING)
    }

    private fun entry(eventId: String) = PendingNotificationProcessing(
        eventId = eventId,
        packageName = "com.orange.money",
        source = TransactionSource.ORANGE_MONEY,
        channel = TransactionChannel.PUSH,
        title = "Orange Money",
        fullText = "Vous avez reçu 5000 FCFA de Raymond",
        postTime = 1_700_000_000_000L,
        receivedAt = 1_700_000_000_000L,
    )

    private fun entity(eventId: String) = PendingNotificationProcessingEntity(
        eventId = eventId,
        packageName = "com.orange.money",
        source = TransactionSource.ORANGE_MONEY,
        channel = TransactionChannel.PUSH,
        title = "Orange Money",
        fullText = "Vous avez reçu 5000 FCFA de Raymond",
        postTime = 1_700_000_000_000L,
        receivedAt = 1_700_000_000_000L,
        status = PendingNotificationStatus.PENDING,
        attemptCount = 0,
        lastError = null,
    )

    private companion object {
        const val EVENT_ID = "event-1"
    }
}
