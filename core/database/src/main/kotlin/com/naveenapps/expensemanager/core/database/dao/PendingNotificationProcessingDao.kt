package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.PendingNotificationProcessingEntity
import com.naveenapps.expensemanager.core.model.PendingNotificationStatus

@Dao
interface PendingNotificationProcessingDao {

    /**
     * `IGNORE` : un `eventId` déjà présent (rejeu Android de la même notification) est conservé
     * tel quel, aucune deuxième entrée n'est créée et la première n'est jamais écrasée.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingNotificationProcessingEntity): Long

    @Query("SELECT * FROM pending_notification_processing WHERE status = :status ORDER BY received_at ASC, event_id ASC")
    suspend fun getByStatus(status: PendingNotificationStatus): List<PendingNotificationProcessingEntity>

    @Query("SELECT * FROM pending_notification_processing WHERE event_id = :eventId LIMIT 1")
    suspend fun getByEventId(eventId: String): PendingNotificationProcessingEntity?

    @Query("UPDATE pending_notification_processing SET status = :status WHERE event_id = :eventId")
    suspend fun updateStatus(eventId: String, status: PendingNotificationStatus)

    @Query(
        "UPDATE pending_notification_processing " +
            "SET status = :pending, attempt_count = attempt_count + 1, last_error = :error " +
            "WHERE event_id = :eventId",
    )
    suspend fun recordFailure(eventId: String, pending: PendingNotificationStatus, error: String?)

    @Query("DELETE FROM pending_notification_processing WHERE event_id = :eventId")
    suspend fun delete(eventId: String)
}
