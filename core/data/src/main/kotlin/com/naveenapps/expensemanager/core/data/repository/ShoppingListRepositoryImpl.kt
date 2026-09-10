package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.mappers.toDomainModel
import com.naveenapps.expensemanager.core.data.mappers.toEntityModel
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.CategoryDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListDao
import com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShoppingListRepositoryImpl(
    private val shoppingListDao: ShoppingListDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val dispatchers: AppCoroutineDispatchers,
) : ShoppingListRepository {

    override fun getShoppingLists(): Flow<List<ShoppingList>> =
        shoppingListDao.getAll().map { entities ->
            entities.orEmpty().map { it.toEnrichedDomainModel() }
        }

    override suspend fun findShoppingListById(id: String): Resource<ShoppingList> =
        withContext(dispatchers.io) {
            return@withContext try {
                val entity = shoppingListDao.findById(id)
                if (entity != null) {
                    Resource.Success(entity.toEnrichedDomainModel())
                } else {
                    Resource.Error(KotlinNullPointerException())
                }
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun addShoppingList(shoppingList: ShoppingList): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                val response = shoppingListDao.insert(shoppingList.toEntityModel())
                Resource.Success(response != -1L)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun updateShoppingList(shoppingList: ShoppingList): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                shoppingListDao.update(shoppingList.toEntityModel())
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    override suspend fun deleteShoppingList(shoppingList: ShoppingList): Resource<Boolean> =
        withContext(dispatchers.io) {
            return@withContext try {
                shoppingListDao.deleteById(shoppingList.id)
                Resource.Success(true)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }

    private suspend fun ShoppingListEntity.toEnrichedDomainModel(): ShoppingList {
        var shoppingList = toDomainModel()
        categoryDao.findById(shoppingList.categoryId)?.let {
            shoppingList = shoppingList.copy(category = it.toDomainModel())
        }
        accountDao.findById(shoppingList.accountId)?.let {
            shoppingList = shoppingList.copy(account = it.toDomainModel())
        }
        return shoppingList
    }
}
