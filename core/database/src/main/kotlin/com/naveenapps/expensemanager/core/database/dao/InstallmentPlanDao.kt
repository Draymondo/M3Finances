package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.InstallmentPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentPlanDao : BaseDao<InstallmentPlanEntity> {

    @Query("DELETE FROM installment_plan")
    suspend fun deleteAll()

    @Query("SELECT * FROM installment_plan ORDER BY created_on DESC")
    fun getAll(): Flow<List<InstallmentPlanEntity>?>

    @Query("SELECT * FROM installment_plan")
    suspend fun getAllInstallmentPlanEntities(): List<InstallmentPlanEntity>

    @Query("SELECT * FROM installment_plan WHERE id = :id")
    suspend fun findById(id: String): InstallmentPlanEntity?

    @Query("SELECT * FROM installment_plan WHERE account_id = :accountId")
    suspend fun findByAccountId(accountId: String): InstallmentPlanEntity?
}
