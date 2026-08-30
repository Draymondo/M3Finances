package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

/**
 * Thin CRUD over the `shopping_list_item` table. A row here always means "still pending" — see
 * [ShoppingListItem] for why there is no checked-flag column. Checking an item off is handled by
 * `CheckOffShoppingListItemUseCase`, which creates the real transaction and then calls
 * [deleteShoppingListItemById] here — this repository itself never creates transactions.
 */
interface ShoppingListItemRepository {

    fun getShoppingListItems(shoppingListId: String): Flow<List<ShoppingListItem>>

    suspend fun findShoppingListItemById(id: String): Resource<ShoppingListItem>

    suspend fun addShoppingListItem(item: ShoppingListItem): Resource<Boolean>

    suspend fun deleteShoppingListItemById(id: String): Resource<Boolean>
}
