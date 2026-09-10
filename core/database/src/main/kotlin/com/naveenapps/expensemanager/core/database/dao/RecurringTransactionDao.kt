package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface RecurringTransactionDao : BaseDao<RecurringTransactionEntity> {

    @Query("DELETE FROM recurring_transaction")
    suspend fun deleteAll()

    @Query("SELECT * FROM recurring_transaction ORDER BY next_occurrence_date ASC")
    fun getAll(): Flow<List<RecurringTransactionEntity>?>

    @Query("SELECT * FROM recurring_transaction")
    suspend fun getAllRecurringTransactionEntities(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transaction WHERE id = :id")
    suspend fun findById(id: String): RecurringTransactionEntity?

    /** Templates due to fire: active, and their next occurrence has arrived (or is overdue —
     * e.g. the app was closed for a few days), and not past their optional end date. */
    @Query(
        """
        SELECT * FROM recurring_transaction
        WHERE is_active = 1
        AND next_occurrence_date <= :now
        AND (end_date IS NULL OR end_date >= next_occurrence_date)
        """,
    )
    suspend fun getDueRecurringTransactions(now: Date): List<RecurringTransactionEntity>

    @Query(
        "UPDATE recurring_transaction SET next_occurrence_date = :nextOccurrenceDate, " +
            "updated_on = :updatedOn WHERE id = :id",
    )
    suspend fun updateNextOccurrence(id: String, nextOccurrenceDate: Date, updatedOn: Date)
}
