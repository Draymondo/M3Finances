package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.ShoppingListItemDao
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShoppingListItemRepositoryImpl(
    private val shoppingListItemDao: ShoppingListItemDao,
    private val dispatchers: AppCoroutineDispatchers,
) : ShoppingListItemRepository {

    override fun getShoppingListItems(shoppingListId: String): Flow<List<ShoppingListItem>> =
        shoppingListItemDao.getForShoppingList(shoppingListId).map { entities ->
            entities.orEmpty().map { it.toDomainModel() }
        }

    override suspend fun findShoppingListItemById(id: String): Resource<ShoppingListItem> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = shoppingListItemDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun addShoppingListItem(item: ShoppingListItem): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = shoppingListItemDao.insert(item.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun deleteShoppingListItemById(id: String): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                shoppingListItemDao.deleteById(id)
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }
}
