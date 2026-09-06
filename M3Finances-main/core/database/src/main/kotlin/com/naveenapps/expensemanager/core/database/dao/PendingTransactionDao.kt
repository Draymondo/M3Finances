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
}

