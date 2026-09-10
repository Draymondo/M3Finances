package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingList
import kotlinx.coroutines.flow.Flow

/**
 * Thin CRUD over the `shopping_list` metadata table. Unlike [DebtRepository], there is no hidden
 * counterparty account here — [ShoppingList.accountId] and [ShoppingList.categoryId] point at
 * real, user-visible rows, and money movement is recorded by ordinary expense transactions when
 * items are checked off (see `CheckOffShoppingListItemUseCase`), not by anything in this
 * repository.
 */
interface ShoppingListRepository {

    /** Enriched with [ShoppingList.category] and [ShoppingList.account] already populated. */
    fun getShoppingLists(): Flow<List<ShoppingList>>

    suspend fun findShoppingListById(id: String): Resource<ShoppingList>

    suspend fun addShoppingList(shoppingList: ShoppingList): Resource<Boolean>

    suspend fun updateShoppingList(shoppingList: ShoppingList): Resource<Boolean>

    suspend fun deleteShoppingList(shoppingList: ShoppingList): Resource<Boolean>
}
