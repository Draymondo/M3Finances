package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao : BaseDao<ShoppingListEntity> {

    @Query("SELECT * FROM shopping_list ORDER BY created_on DESC")
    fun getAll(): Flow<List<ShoppingListEntity>?>

    @Query("SELECT * FROM shopping_list WHERE id = :id")
    suspend fun findById(id: String): ShoppingListEntity?

    @Query("SELECT * FROM shopping_list ORDER BY created_on DESC")
    suspend fun getAllEntities(): List<ShoppingListEntity>

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_list")
    suspend fun deleteAll()
}
