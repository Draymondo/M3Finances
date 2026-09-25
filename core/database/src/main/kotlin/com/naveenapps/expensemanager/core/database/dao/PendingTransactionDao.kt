package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.naveenapps.expensemanager.core.database.entity.PendingTransactionEntity
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.model.isKnownProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    /**
     * Returns only actionable pending transactions:
     * - SMS-captured rows (scheduled_date IS NULL)
     * - Scheduled rows whose date has been reached (scheduled_date <= :now)
     */
    @Query("SELECT * FROM pending_transaction WHERE scheduled_date IS NULL OR scheduled_date <= :now ORDER BY created_on DESC")
    fun getActionablePendingTransactions(now: Long): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transaction ORDER BY created_on DESC")
    fun getAllPendingTransactions(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transaction")
    suspend fun getAllPendingTransactionEntities(): List<PendingTransactionEntity>

    @Query("SELECT * FROM pending_transaction WHERE id = :id LIMIT 1")
    fun getPendingTransactionById(id: String): Flow<PendingTransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: PendingTransactionEntity)

    @Update
    suspend fun update(transaction: PendingTransactionEntity)

    @Query(
        "SELECT * FROM pending_transaction " +
            "WHERE source = :source AND provider_reference = :providerReference LIMIT 1",
    )
    suspend fun getPendingTransactionByProviderReference(
        source: TransactionSource,
        providerReference: String,
    ): PendingTransactionEntity?

    @Query("SELECT * FROM pending_transaction WHERE deduplication_key = :deduplicationKey")
    suspend fun getPendingTransactionsByDeduplicationKey(deduplicationKey: String): List<PendingTransactionEntity>

    @Query("SELECT * FROM pending_transaction WHERE notification_event_id = :eventId LIMIT 1")
    suspend fun getPendingTransactionByEventId(eventId: String): PendingTransactionEntity?

    @Transaction
    suspend fun insertWithDeduplication(
        entity: PendingTransactionEntity,
        crossChannelWindowMs: Long = 90_000L,
    ): Boolean {
        // 1. Idempotence : si notification_event_id est déjà présent
        val eventId = entity.notificationEventId
        if (!eventId.isNullOrBlank()) {
            val byEvent = getPendingTransactionByEventId(eventId)
            if (byEvent != null) {
                return true
            }
        }

        // 2. Référence fournisseur : même fournisseur (connu) + même référence
        val providerRef = entity.providerReference
        if (!providerRef.isNullOrBlank() && entity.source.isKnownProvider()) {
            val byRef = getPendingTransactionByProviderReference(entity.source, providerRef)
            if (byRef != null) {
                return true
            }
            insert(entity)
            return true
        }

        // 3. Sans référence : fusion uniquement entre canaux différents, fournisseur connu et identique
        // des deux côtés, même identité financière, distance réelle < 90 s. Jamais dans le même canal.
        // Fournisseur inconnu : on garde les deux.
        val dedupKey = entity.deduplicationKey
        if (!dedupKey.isNullOrBlank() &&
            entity.source.isKnownProvider() &&
            entity.channel != null &&
            entity.channel != TransactionChannel.UNKNOWN
        ) {
            val existingList = getPendingTransactionsByDeduplicationKey(dedupKey)
            val crossChannelDuplicate = existingList.firstOrNull { existing ->
                existing.source == entity.source &&
                    existing.channel != null &&
                    existing.channel != TransactionChannel.UNKNOWN &&
                    existing.channel != entity.channel &&
                    kotlin.math.abs(existing.date - entity.date) < crossChannelWindowMs
            }
            if (crossChannelDuplicate != null) {
                return true
            }
        }

        insert(entity)
        return true
    }

    @Query("DELETE FROM pending_transaction WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("DELETE FROM pending_transaction")
    suspend fun deleteAll()

    @Query("UPDATE pending_transaction SET scheduled_date = :newDate WHERE id = :id")
    suspend fun updateScheduledDate(id: String, newDate: Long)

    /**
     * Count of scheduled transactions that are now due (scheduled_date reached).
     */
    @Query("SELECT COUNT(*) FROM pending_transaction WHERE scheduled_date IS NOT NULL AND scheduled_date <= :now")
    suspend fun countScheduledDue(now: Long): Int
}
