package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao : BaseDao<DebtEntity> {

    @Query("DELETE FROM debt")
    suspend fun deleteAll()

    @Query("SELECT * FROM debt ORDER BY created_on DESC")
    fun getAll(): Flow<List<DebtEntity>?>

    @Query("SELECT * FROM debt")
    suspend fun getAllDebtEntities(): List<DebtEntity>

    @Query("SELECT * FROM debt WHERE id = :id")
    suspend fun findById(id: String): DebtEntity?

    @Query("SELECT * FROM debt WHERE account_id = :accountId")
    suspend fun findByAccountId(accountId: String): DebtEntity?
}
