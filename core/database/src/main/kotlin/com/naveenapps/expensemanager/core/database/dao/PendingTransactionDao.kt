package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.naveenapps.expensemanager.core.database.entity.PendingTransactionEntity
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PendingTransactionEntity)

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
