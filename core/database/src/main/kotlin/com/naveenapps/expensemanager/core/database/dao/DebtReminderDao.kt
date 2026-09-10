package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.DebtReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtReminderDao : BaseDao<DebtReminderEntity> {

    @Query("SELECT * FROM debt_reminder WHERE debt_id = :debtId ORDER BY reminder_date ASC")
    fun getForDebt(debtId: String): Flow<List<DebtReminderEntity>?>

    /** All reminders across every debt — used to reconcile scheduled WorkManager jobs against
     * the current state of the world (see `DebtReminderTrigger`). */
    @Query("SELECT * FROM debt_reminder")
    fun getAll(): Flow<List<DebtReminderEntity>?>

    @Query("SELECT * FROM debt_reminder")
    suspend fun getAllDebtReminderEntities(): List<DebtReminderEntity>

    @Query("DELETE FROM debt_reminder WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM debt_reminder")
    suspend fun deleteAll()
}
