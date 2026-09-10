package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naveenapps.expensemanager.core.database.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListItemDao : BaseDao<ShoppingListItemEntity> {

    @Query("SELECT * FROM shopping_list_item WHERE shopping_list_id = :shoppingListId ORDER BY created_on ASC")
    fun getForShoppingList(shoppingListId: String): Flow<List<ShoppingListItemEntity>?>

    @Query("SELECT * FROM shopping_list_item WHERE id = :id")
    suspend fun findById(id: String): ShoppingListItemEntity?

    @Query("SELECT * FROM shopping_list_item ORDER BY created_on ASC")
    suspend fun getAllEntities(): List<ShoppingListItemEntity>

    @Query("DELETE FROM shopping_list_item WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_list_item")
    suspend fun deleteAll()
}
