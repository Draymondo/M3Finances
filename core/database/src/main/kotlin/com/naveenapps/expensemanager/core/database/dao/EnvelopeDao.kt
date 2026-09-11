package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.EnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao : BaseDao<EnvelopeEntity> {

    @Query("SELECT * FROM envelope ORDER BY created_on DESC")
    fun getEnvelopes(): Flow<List<EnvelopeEntity>?>

    /** Snapshot variant of [getEnvelopes] for the cloud sync. */
    @Query("SELECT * FROM envelope ORDER BY created_on DESC")
    suspend fun getAllEnvelopeEntities(): List<EnvelopeEntity>

    @Query("SELECT * FROM envelope WHERE id = :id")
    suspend fun findById(id: String): EnvelopeEntity?

    @Query("SELECT * FROM envelope WHERE id = :id")
    fun findByIdFlow(id: String): Flow<EnvelopeEntity?>

    /** All envelopes active for a given category + period key — normally 0 or 1 row, since
     * creation is blocked from producing duplicates, but the check itself (and any legacy data)
     * should not assume that. */
    @Query(
        "SELECT * FROM envelope WHERE category_id = :categoryId AND selected_month = :selectedMonth AND period_type = :periodType",
    )
    suspend fun findByCategoryAndPeriod(
        categoryId: String,
        selectedMonth: String,
        periodType: Int,
    ): List<EnvelopeEntity>

    /** Every envelope active for the given period key, regardless of category — used to build
     * the virtual general-budget fallback (sum of active envelopes) when no classic
     * "all categories" budget exists for that period. */
    @Query("SELECT * FROM envelope WHERE selected_month = :selectedMonth AND period_type = :periodType")
    suspend fun findAllByPeriod(selectedMonth: String, periodType: Int): List<EnvelopeEntity>

    @Query("DELETE FROM envelope WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM envelope")
    suspend fun deleteAll()
}
