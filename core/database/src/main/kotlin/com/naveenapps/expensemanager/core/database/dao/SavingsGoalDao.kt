package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao : BaseDao<SavingsGoalEntity> {

    @Query("DELETE FROM savings_goal")
    suspend fun deleteAll()

    @Query("SELECT * FROM savings_goal ORDER BY created_on DESC")
    fun getAll(): Flow<List<SavingsGoalEntity>?>

    @Query("SELECT * FROM savings_goal")
    suspend fun getAllSavingsGoalEntities(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goal WHERE id = :id")
    suspend fun findById(id: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goal WHERE account_id = :accountId")
    suspend fun findByAccountId(accountId: String): SavingsGoalEntity?
}
