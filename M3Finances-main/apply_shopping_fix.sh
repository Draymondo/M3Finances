#!/data/data/com.termux/files/usr/bin/bash
# Script de correctif : ajoute la couche métier manquante pour feature/shopping
# (core:model / database / repository / data / domain) et branche tout dans les
# modules Koin existants. A executer depuis la racine du repo (~/repo).
set -euo pipefail

if [ ! -f "settings.gradle.kts" ]; then
  echo "Erreur : lance ce script depuis la racine du repo (là où se trouve settings.gradle.kts)."
  exit 1
fi

echo "== Création des nouveaux fichiers =="

echo "  core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/ShoppingList.kt"
mkdir -p "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model"
cat > "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/ShoppingList.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.model

import androidx.compose.runtime.Stable
import java.util.Date

/**
 * A named shopping list tied to a real expense [Category] and a real [Account] — unlike [Debt]
 * or a savings goal, this never creates a hidden counterparty account: checking an item off
 * (see [ShoppingListItem]) records an ordinary [TransactionType.EXPENSE] straight against
 * [accountId], so the list integrates with existing budgets automatically.
 */
@Stable
data class ShoppingList(
    val id: String,
    val name: String,
    val categoryId: String,
    val accountId: String,
    val createdOn: Date,
    val updatedOn: Date,
    /** Populated for display by the domain layer; empty/default until then. */
    val category: Category = Category(
        id = "",
        name = "",
        type = CategoryType.EXPENSE,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        createdOn = Date(),
        updatedOn = Date(),
    ),
    val account: Account = Account(
        id = "",
        name = "",
        type = AccountType.REGULAR,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        createdOn = Date(),
        updatedOn = Date(),
    ),
)

/**
 * A single pending item on a [ShoppingList]. There is no "checked" flag: checking an item off
 * immediately turns it into a real [Transaction] (see `CheckOffShoppingListItemUseCase`) and
 * deletes this row — a row existing at all means it's still pending. Unchecked items stay in the
 * list indefinitely.
 */
@Stable
data class ShoppingListItem(
    val id: String,
    val shoppingListId: String,
    val name: String,
    val price: Double,
    val createdOn: Date,
)
CLAUDE_FIX_EOF

echo "  core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity/ShoppingListEntity.kt"
mkdir -p "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity"
cat > "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity/ShoppingListEntity.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "shopping_list",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("category_id"),
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("account_id"),
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "created_on")
    val createdOn: Date,
    @ColumnInfo(name = "updated_on")
    val updatedOn: Date,
)
CLAUDE_FIX_EOF

echo "  core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity/ShoppingListItemEntity.kt"
mkdir -p "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity"
cat > "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity/ShoppingListItemEntity.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/** A row here always means "still pending" — checking an item off deletes it (see
 * `ShoppingListItem` for why there is no checked-flag column). */
@Entity(
    tableName = "shopping_list_item",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("shopping_list_id"),
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "shopping_list_id")
    val shoppingListId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "price")
    val price: Double,
    @ColumnInfo(name = "created_on")
    val createdOn: Date,
)
CLAUDE_FIX_EOF

echo "  core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/dao/ShoppingListDao.kt"
mkdir -p "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/dao"
cat > "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/dao/ShoppingListDao.kt" << 'CLAUDE_FIX_EOF'
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

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteById(id: String)
}
CLAUDE_FIX_EOF

echo "  core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/dao/ShoppingListItemDao.kt"
mkdir -p "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/dao"
cat > "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/dao/ShoppingListItemDao.kt" << 'CLAUDE_FIX_EOF'
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

    @Query("DELETE FROM shopping_list_item WHERE id = :id")
    suspend fun deleteById(id: String)
}
CLAUDE_FIX_EOF

echo "  core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository/ShoppingListRepository.kt"
mkdir -p "core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository"
cat > "core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository/ShoppingListRepository.kt" << 'CLAUDE_FIX_EOF'
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
CLAUDE_FIX_EOF

echo "  core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository/ShoppingListItemRepository.kt"
mkdir -p "core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository"
cat > "core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository/ShoppingListItemRepository.kt" << 'CLAUDE_FIX_EOF'
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
CLAUDE_FIX_EOF

echo "  core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/mappers/ShoppingListMappers.kt"
mkdir -p "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/mappers"
cat > "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/mappers/ShoppingListMappers.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity
import com.naveenapps.expensemanager.core.database.entity.ShoppingListItemEntity
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.ShoppingListItem

fun ShoppingList.toEntityModel(): ShoppingListEntity {
    return ShoppingListEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun ShoppingListEntity.toDomainModel(): ShoppingList {
    return ShoppingList(
        id = id,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun ShoppingListItem.toEntityModel(): ShoppingListItemEntity {
    return ShoppingListItemEntity(
        id = id,
        shoppingListId = shoppingListId,
        name = name,
        price = price,
        createdOn = createdOn,
    )
}

fun ShoppingListItemEntity.toDomainModel(): ShoppingListItem {
    return ShoppingListItem(
        id = id,
        shoppingListId = shoppingListId,
        name = name,
        price = price,
        createdOn = createdOn,
    )
}
CLAUDE_FIX_EOF

echo "  core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/ShoppingListRepositoryImpl.kt"
mkdir -p "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository"
cat > "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/ShoppingListRepositoryImpl.kt" << 'CLAUDE_FIX_EOF'
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
CLAUDE_FIX_EOF

echo "  core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/ShoppingListItemRepositoryImpl.kt"
mkdir -p "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository"
cat > "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/ShoppingListItemRepositoryImpl.kt" << 'CLAUDE_FIX_EOF'
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
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/GetShoppingListsUseCase.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/GetShoppingListsUseCase.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

/**
 * Lists every shopping list along with how many items are still pending and their combined
 * price, so the list screen doesn't need to open each list to show a preview. Re-emits whenever
 * any list's pending items change (checking an item off elsewhere updates this automatically).
 */
class GetShoppingListsUseCase(
    private val repository: ShoppingListRepository,
    private val itemRepository: ShoppingListItemRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(): Flow<List<ShoppingListUiModel>> {
        return repository.getShoppingLists()
            .flatMapLatest { shoppingLists ->
                if (shoppingLists.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList())
                }
                combine(
                    shoppingLists.map { shoppingList ->
                        itemRepository.getShoppingListItems(shoppingList.id)
                    },
                ) { itemLists ->
                    shoppingLists.mapIndexed { index, shoppingList ->
                        shoppingList to itemLists[index]
                    }
                }
            }
            .combine(getCurrencyUseCase.invoke()) { pairs, currency ->
                pairs.map { (shoppingList, items) ->
                    ShoppingListUiModel(
                        shoppingList = shoppingList,
                        pendingItemCount = items.size,
                        pendingTotal = getFormattedAmountUseCase.invoke(
                            items.sumOf { it.price },
                            currency,
                        ),
                    )
                }
            }
            .flowOn(appCoroutineDispatchers.computation)
    }
}

@Stable
data class ShoppingListUiModel(
    val shoppingList: ShoppingList,
    val pendingItemCount: Int,
    val pendingTotal: Amount,
)
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/ShoppingListCrudUseCases.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/ShoppingListCrudUseCases.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository

class FindShoppingListByIdUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(id: String): Resource<ShoppingList> {
        return repository.findShoppingListById(id)
    }
}

class AddShoppingListUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(shoppingList: ShoppingList): Resource<Boolean> {
        if (shoppingList.name.isBlank()) {
            return Resource.Error(Exception("Name shouldn't be blank"))
        }
        return repository.addShoppingList(shoppingList)
    }
}

class UpdateShoppingListUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(shoppingList: ShoppingList): Resource<Boolean> {
        if (shoppingList.name.isBlank()) {
            return Resource.Error(Exception("Name shouldn't be blank"))
        }
        return repository.updateShoppingList(shoppingList)
    }
}

/** Deleting a shopping list only removes its metadata row and, via the `shopping_list_item`
 * table's `ON DELETE CASCADE` foreign key, its pending items. It never touches transactions or
 * accounts — items only ever become transactions when explicitly checked off. */
class DeleteShoppingListUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(shoppingList: ShoppingList): Resource<Boolean> {
        return repository.deleteShoppingList(shoppingList)
    }
}
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/ShoppingListItemUseCases.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/ShoppingListItemUseCases.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import kotlinx.coroutines.flow.Flow

class GetShoppingListItemsUseCase(
    private val repository: ShoppingListItemRepository,
) {
    operator fun invoke(shoppingListId: String): Flow<List<ShoppingListItem>> {
        return repository.getShoppingListItems(shoppingListId)
    }
}

class AddShoppingListItemUseCase(
    private val repository: ShoppingListItemRepository,
) {
    suspend operator fun invoke(item: ShoppingListItem): Resource<Boolean> {
        if (item.name.isBlank()) {
            return Resource.Error(Exception("Name shouldn't be blank"))
        }
        if (item.price < 0.0) {
            return Resource.Error(Exception("Price can't be negative"))
        }
        return repository.addShoppingListItem(item)
    }
}

/** Removes a pending item without buying it — no transaction is created. Use
 * [CheckOffShoppingListItemUseCase] to record the purchase instead. */
class DeleteShoppingListItemUseCase(
    private val repository: ShoppingListItemRepository,
) {
    suspend operator fun invoke(itemId: String): Resource<Boolean> {
        return repository.deleteShoppingListItemById(itemId)
    }
}
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/CheckOffShoppingListItemUseCase.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/CheckOffShoppingListItemUseCase.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository
import java.util.UUID

/**
 * Checking an item off is the whole point of the feature: it immediately turns a pending
 * [com.naveenapps.expensemanager.core.model.ShoppingListItem] into a real
 * [TransactionType.EXPENSE] against the shopping list's own real account and category — no
 * hidden account, no "finish shopping" step — then deletes the item. This is what makes the list
 * integrate automatically with the existing budgets: the transaction is completely ordinary.
 */
class CheckOffShoppingListItemUseCase(
    private val itemRepository: ShoppingListItemRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
) {
    suspend operator fun invoke(itemId: String): Resource<Boolean> {
        val itemResult = itemRepository.findShoppingListItemById(itemId)
        val item = when (itemResult) {
            is Resource.Error -> return itemResult
            is Resource.Success -> itemResult.data
        }

        val shoppingListResult = shoppingListRepository.findShoppingListById(item.shoppingListId)
        val shoppingList = when (shoppingListResult) {
            is Resource.Error -> return shoppingListResult
            is Resource.Success -> shoppingListResult.data
        }

        val transactionResult = addTransactionUseCase.invoke(
            Transaction(
                id = UUID.randomUUID().toString(),
                notes = item.name,
                categoryId = shoppingList.categoryId,
                fromAccountId = shoppingList.accountId,
                toAccountId = null,
                amount = Amount(item.price),
                imagePath = "",
                type = TransactionType.EXPENSE,
                createdOn = item.createdOn,
                updatedOn = item.createdOn,
            ),
        )
        if (transactionResult is Resource.Error) {
            return transactionResult
        }

        return itemRepository.deleteShoppingListItemById(itemId)
    }
}
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/ShoppingListUseCaseModule.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/shoppinglist/ShoppingListUseCaseModule.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import org.koin.dsl.module

val ShoppingListUseCaseModule = module {
    single {
        GetShoppingListsUseCase(
            repository = get(),
            itemRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            appCoroutineDispatchers = get(),
        )
    }
    single { FindShoppingListByIdUseCase(repository = get()) }
    single { AddShoppingListUseCase(repository = get()) }
    single { UpdateShoppingListUseCase(repository = get()) }
    single { DeleteShoppingListUseCase(repository = get()) }

    single { GetShoppingListItemsUseCase(repository = get()) }
    single { AddShoppingListItemUseCase(repository = get()) }
    single { DeleteShoppingListItemUseCase(repository = get()) }
    single {
        CheckOffShoppingListItemUseCase(
            itemRepository = get(),
            shoppingListRepository = get(),
            addTransactionUseCase = get(),
        )
    }
}
CLAUDE_FIX_EOF

echo
echo "== Application des correctifs sur les fichiers existants =="
python3 - << 'CLAUDE_PY_EOF'
import sys

EDITS = [
  # (file, old, new)
  (
    "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/di/UseCaseModule.kt",
    "import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.SavingsGoalUseCaseModule\nimport com.naveenapps.expensemanager.core.domain.usecase.settings.SettingsUseCaseModule",
    "import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.SavingsGoalUseCaseModule\nimport com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.ShoppingListUseCaseModule\nimport com.naveenapps.expensemanager.core.domain.usecase.settings.SettingsUseCaseModule",
  ),
  (
    "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/di/UseCaseModule.kt",
    "        DebtUseCaseModule,\n        SavingsGoalUseCaseModule,\n        NetWorthUseCaseModule,\n    )",
    "        DebtUseCaseModule,\n        SavingsGoalUseCaseModule,\n        NetWorthUseCaseModule,\n        ShoppingListUseCaseModule,\n    )",
  ),
  (
    "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di/KoinRepositoryModule.kt",
    "import com.naveenapps.expensemanager.core.data.repository.SavingsGoalRepositoryImpl\nimport com.naveenapps.expensemanager.core.data.repository.CountryRepositoryImpl",
    "import com.naveenapps.expensemanager.core.data.repository.SavingsGoalRepositoryImpl\nimport com.naveenapps.expensemanager.core.data.repository.ShoppingListRepositoryImpl\nimport com.naveenapps.expensemanager.core.data.repository.ShoppingListItemRepositoryImpl\nimport com.naveenapps.expensemanager.core.data.repository.CountryRepositoryImpl",
  ),
  (
    "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di/KoinRepositoryModule.kt",
    "import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository\nimport org.koin.android.ext.koin.androidContext",
    "import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository\nimport com.naveenapps.expensemanager.core.repository.ShoppingListRepository\nimport com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository\nimport org.koin.android.ext.koin.androidContext",
  ),
  (
    "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di/KoinRepositoryModule.kt",
    "    single<SavingsGoalRepository> {\n        SavingsGoalRepositoryImpl(\n            savingsGoalDao = get(),\n            accountDao = get(),\n            dispatchers = get(),\n        )\n    }\n\n    // Sauvegarde cloud automatique",
    "    single<SavingsGoalRepository> {\n        SavingsGoalRepositoryImpl(\n            savingsGoalDao = get(),\n            accountDao = get(),\n            dispatchers = get(),\n        )\n    }\n    single<ShoppingListRepository> {\n        ShoppingListRepositoryImpl(\n            shoppingListDao = get(),\n            categoryDao = get(),\n            accountDao = get(),\n            dispatchers = get(),\n        )\n    }\n    single<ShoppingListItemRepository> {\n        ShoppingListItemRepositoryImpl(\n            shoppingListItemDao = get(),\n            dispatchers = get(),\n        )\n    }\n\n    // Sauvegarde cloud automatique",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt",
    "import com.naveenapps.expensemanager.core.database.dao.SavingsGoalDao\nimport com.naveenapps.expensemanager.core.database.dao.RecurringTransactionDao\nimport com.naveenapps.expensemanager.core.database.dao.TransactionDao",
    "import com.naveenapps.expensemanager.core.database.dao.SavingsGoalDao\nimport com.naveenapps.expensemanager.core.database.dao.ShoppingListDao\nimport com.naveenapps.expensemanager.core.database.dao.ShoppingListItemDao\nimport com.naveenapps.expensemanager.core.database.dao.RecurringTransactionDao\nimport com.naveenapps.expensemanager.core.database.dao.TransactionDao",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt",
    "import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity\nimport com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity\nimport com.naveenapps.expensemanager.core.database.entity.TransactionEntity",
    "import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity\nimport com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity\nimport com.naveenapps.expensemanager.core.database.entity.ShoppingListItemEntity\nimport com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity\nimport com.naveenapps.expensemanager.core.database.entity.TransactionEntity",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt",
    "        SavingsGoalEntity::class,\n        TransactionSplitItemEntity::class,\n    ],\n    version = 11,\n    exportSchema = true,\n)",
    "        SavingsGoalEntity::class,\n        TransactionSplitItemEntity::class,\n        ShoppingListEntity::class,\n        ShoppingListItemEntity::class,\n    ],\n    version = 12,\n    exportSchema = true,\n)",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt",
    "    abstract fun savingsGoalDao(): SavingsGoalDao\n}",
    "    abstract fun savingsGoalDao(): SavingsGoalDao\n\n    abstract fun shoppingListDao(): ShoppingListDao\n\n    abstract fun shoppingListItemDao(): ShoppingListItemDao\n}",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/DatabaseMigrations.kt",
    '                "FOREIGN KEY(`transaction_id`) REFERENCES `transaction`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +\n                "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",\n        )\n    }\n}',
    '                "FOREIGN KEY(`transaction_id`) REFERENCES `transaction`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +\n                "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",\n        )\n    }\n}\n\n/**\n * Adds the `shopping_list` and `shopping_list_item` tables. Unlike `debt`/`savings_goal`, a\n * shopping list points straight at a real `category`/`account` — no hidden counterparty account\n * is created. An item row always means "still pending"; checking one off deletes its row and\n * creates an ordinary expense transaction instead of flipping a checked flag — see the\n * `ShoppingList`/`ShoppingListItem` models for the full design rationale.\n */\ninternal val MIGRATION_11_12 = object : Migration(11, 12) {\n    override fun migrate(db: SupportSQLiteDatabase) {\n        db.execSQL(\n            "CREATE TABLE IF NOT EXISTS `shopping_list` (" +\n                "`id` TEXT NOT NULL, " +\n                "`name` TEXT NOT NULL, " +\n                "`category_id` TEXT NOT NULL, " +\n                "`account_id` TEXT NOT NULL, " +\n                "`created_on` INTEGER NOT NULL, " +\n                "`updated_on` INTEGER NOT NULL, " +\n                "PRIMARY KEY(`id`), " +\n                "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +\n                "FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",\n        )\n        db.execSQL(\n            "CREATE TABLE IF NOT EXISTS `shopping_list_item` (" +\n                "`id` TEXT NOT NULL, " +\n                "`shopping_list_id` TEXT NOT NULL, " +\n                "`name` TEXT NOT NULL, " +\n                "`price` REAL NOT NULL, " +\n                "`created_on` INTEGER NOT NULL, " +\n                "PRIMARY KEY(`id`), " +\n                "FOREIGN KEY(`shopping_list_id`) REFERENCES `shopping_list`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",\n        )\n    }\n}',
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/di/KoinDatabaseModule.kt",
    "import com.naveenapps.expensemanager.core.database.MIGRATION_10_11\nimport org.koin.android.ext.koin.androidContext",
    "import com.naveenapps.expensemanager.core.database.MIGRATION_10_11\nimport com.naveenapps.expensemanager.core.database.MIGRATION_11_12\nimport org.koin.android.ext.koin.androidContext",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/di/KoinDatabaseModule.kt",
    "            MIGRATION_10_11,\n        ).build()\n    }",
    "            MIGRATION_10_11,\n            MIGRATION_11_12,\n        ).build()\n    }",
  ),
  (
    "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/di/KoinDatabaseModule.kt",
    "    single { get<ExpenseManagerDatabase>().savingsGoalDao() }\n}",
    "    single { get<ExpenseManagerDatabase>().savingsGoalDao() }\n    single { get<ExpenseManagerDatabase>().shoppingListDao() }\n    single { get<ExpenseManagerDatabase>().shoppingListItemDao() }\n}",
  ),
]

def main():
    ok = True
    for path, old, new in EDITS:
        with open(path, "r", encoding="utf-8") as fh:
            content = fh.read()
        if new in content:
            print(f"  [déjà appliqué] {path}")
            continue
        count = content.count(old)
        if count == 0:
            print(f"  [ERREUR] motif introuvable dans {path} — le fichier a peut-être déjà été modifié différemment.")
            print(f"           Vérifie ce fichier à la main. Motif recherché :")
            print("           " + old.splitlines()[0][:80])
            ok = False
            continue
        if count > 1:
            print(f"  [ERREUR] motif trouvé {count} fois dans {path} (attendu 1) — édition ambigüe, abandon pour ce fichier.")
            ok = False
            continue
        content = content.replace(old, new, 1)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(content)
        print(f"  [ok] {path}")
    if not ok:
        print("\nCertains correctifs n'ont pas pu être appliqués automatiquement (voir ERREUR ci-dessus).")
        sys.exit(1)

if __name__ == "__main__":
    main()
CLAUDE_PY_EOF

echo
echo "== Fichiers touchés : =="
git status --short | grep -i shopping || true
git status --short -- core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/di/UseCaseModule.kt core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di/KoinRepositoryModule.kt core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/DatabaseMigrations.kt core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/di/KoinDatabaseModule.kt
echo
echo "Correctif appliqué. Étapes suivantes :"
echo "  git add -A"
echo "  git commit -m \"Fix: couche métier manquante pour feature/shopping\""
echo "  git push"
