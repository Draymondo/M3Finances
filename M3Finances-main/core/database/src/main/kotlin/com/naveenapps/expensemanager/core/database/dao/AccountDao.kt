package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao : BaseDao<AccountEntity> {

    @Query("SELECT * FROM account ORDER BY sequence ASC")
    fun getAccounts(): Flow<List<AccountEntity>?>

    /** Snapshot variant of [getAccounts] for the cloud sync, which needs a one-shot read rather
     * than a reactive stream. */
    @Query("SELECT * FROM account ORDER BY sequence ASC")
    suspend fun getAllAccountEntities(): List<AccountEntity>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun findById(id: String): AccountEntity?

    @Query("DELETE FROM account")
    suspend fun deleteAll()
}
