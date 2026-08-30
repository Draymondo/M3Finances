#!/data/data/com.termux/files/usr/bin/bash
# Etend la sauvegarde cloud (Firestore) aux listes de courses et leurs articles,
# qui n'etaient pas encore couvertes. Ecrase entierement chaque fichier liste
# ci-dessous avec son contenu final complet.
# A executer depuis la racine du repo (~/repo).
set -euo pipefail

if [ ! -f "settings.gradle.kts" ]; then
  echo "Erreur : lance ce script depuis la racine du repo (la ou se trouve settings.gradle.kts)."
  exit 1
fi

echo "== Ecriture des fichiers =="

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

    /** One-shot fetch of every row, used by `CloudBackupRepositoryImpl` — the other queries here
     * are reactive `Flow`s for the UI, which isn't what a one-time backup sync needs. */
    @Query("SELECT * FROM shopping_list")
    suspend fun getAllShoppingListEntities(): List<ShoppingListEntity>

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

    /** One-shot fetch of every row across every list, used by `CloudBackupRepositoryImpl` —
     * [getForShoppingList] is scoped to one list and reactive, neither of which a one-time
     * backup sync needs. */
    @Query("SELECT * FROM shopping_list_item")
    suspend fun getAllShoppingListItemEntities(): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_item WHERE id = :id")
    suspend fun findById(id: String): ShoppingListItemEntity?

    @Query("DELETE FROM shopping_list_item WHERE id = :id")
    suspend fun deleteById(id: String)
}
CLAUDE_FIX_EOF

echo "  core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/cloudbackup/FirestoreEntityMappers.kt"
mkdir -p "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/cloudbackup"
cat > "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/cloudbackup/FirestoreEntityMappers.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.data.cloudbackup

import com.naveenapps.expensemanager.core.database.entity.AccountEntity
import com.naveenapps.expensemanager.core.database.entity.BudgetAccountEntity
import com.naveenapps.expensemanager.core.database.entity.BudgetCategoryEntity
import com.naveenapps.expensemanager.core.database.entity.BudgetEntity
import com.naveenapps.expensemanager.core.database.entity.CategoryEntity
import com.naveenapps.expensemanager.core.database.entity.DebtEntity
import com.naveenapps.expensemanager.core.database.entity.DebtReminderEntity
import com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity
import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity
import com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity
import com.naveenapps.expensemanager.core.database.entity.ShoppingListItemEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionSplitItemEntity
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.DebtDirection
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.TransactionType
import java.util.Date
import java.util.UUID

/**
 * Room entity <-> Firestore document (`Map<String, Any?>`) conversions, one pair of functions
 * per entity. Firestore has no `Date`/enum types, so dates become epoch millis (Long) and enums
 * become their name (String, more readable/robust across schema changes than a bare ordinal —
 * unlike the local SQLite columns, nothing else depends on these ordinals, so there's no reason
 * to inherit that fragility here).
 *
 * Deliberately NOT using Firestore's `@DocumentId`/POJO auto-mapping (data classes with default
 * constructors etc.) to keep this fully explicit and avoid a second, parallel set of model
 * classes — these functions are the single place that would need updating if an entity's schema
 * changes.
 */

fun AccountEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "type" to type.name,
    "iconBackgroundColor" to iconBackgroundColor,
    "iconName" to iconName,
    "amount" to amount,
    "creditLimit" to creditLimit,
    "sequence" to sequence,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
)

fun accountEntityFromFirestoreMap(id: String, map: Map<String, Any?>): AccountEntity = AccountEntity(
    id = id,
    name = map["name"] as? String ?: "",
    type = (map["type"] as? String)?.let { AccountType.valueOf(it) } ?: AccountType.REGULAR,
    iconBackgroundColor = map["iconBackgroundColor"] as? String ?: "",
    iconName = map["iconName"] as? String ?: "",
    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
    creditLimit = (map["creditLimit"] as? Number)?.toDouble() ?: 0.0,
    sequence = (map["sequence"] as? Number)?.toInt() ?: 0,
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
)

fun CategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "type" to type.name,
    "iconBackgroundColor" to iconBackgroundColor,
    "iconName" to iconName,
    "updatedOn" to updatedOn.time,
    "createdOn" to createdOn.time,
    "defaultCategoryKey" to defaultCategoryKey,
)

fun categoryEntityFromFirestoreMap(id: String, map: Map<String, Any?>): CategoryEntity = CategoryEntity(
    id = id,
    name = map["name"] as? String ?: "",
    type = (map["type"] as? String)?.let { CategoryType.valueOf(it) } ?: CategoryType.EXPENSE,
    iconBackgroundColor = map["iconBackgroundColor"] as? String ?: "",
    iconName = map["iconName"] as? String ?: "",
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    defaultCategoryKey = map["defaultCategoryKey"] as? String,
)

fun TransactionEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "notes" to notes,
    "categoryId" to categoryId,
    "fromAccountId" to fromAccountId,
    "type" to type.name,
    "amount" to amount,
    "imagePath" to imagePath,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
    "toAccountId" to toAccountId,
)

fun transactionEntityFromFirestoreMap(id: String, map: Map<String, Any?>): TransactionEntity = TransactionEntity(
    id = id,
    notes = map["notes"] as? String ?: "",
    categoryId = map["categoryId"] as? String ?: "",
    fromAccountId = map["fromAccountId"] as? String ?: "",
    type = (map["type"] as? String)?.let { TransactionType.valueOf(it) } ?: TransactionType.EXPENSE,
    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
    imagePath = map["imagePath"] as? String ?: "",
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
    toAccountId = map["toAccountId"] as? String,
)

fun TransactionSplitItemEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "transactionId" to transactionId,
    "categoryId" to categoryId,
    "amount" to amount,
    "notes" to notes,
)

fun transactionSplitItemEntityFromFirestoreMap(
    id: String,
    map: Map<String, Any?>,
): TransactionSplitItemEntity = TransactionSplitItemEntity(
    id = id,
    transactionId = map["transactionId"] as? String ?: "",
    categoryId = map["categoryId"] as? String ?: "",
    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
    notes = map["notes"] as? String,
)

/**
 * Budgets fold their two Room relation tables (`budget_account_relation`,
 * `budget_category_relation`) into plain id lists on the same document — Firestore has no need
 * for that normalization, it was only there to satisfy SQL's relational model.
 */
fun BudgetEntity.toFirestoreMap(categoryIds: List<String>, accountIds: List<String>): Map<String, Any?> = mapOf(
    "selectedMonth" to selectedMonth,
    "amount" to amount,
    "periodType" to periodType,
    "isAllAccountsSelected" to isAllAccountsSelected,
    "isAllCategoriesSelected" to isAllCategoriesSelected,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
    "categoryIds" to categoryIds,
    "accountIds" to accountIds,
)

fun budgetEntityFromFirestoreMap(id: String, map: Map<String, Any?>): BudgetEntity = BudgetEntity(
    id = id,
    selectedMonth = map["selectedMonth"] as? String ?: "",
    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
    periodType = (map["periodType"] as? Number)?.toInt() ?: 0,
    isAllAccountsSelected = map["isAllAccountsSelected"] as? Boolean ?: false,
    isAllCategoriesSelected = map["isAllCategoriesSelected"] as? Boolean ?: false,
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
)

@Suppress("UNCHECKED_CAST")
fun budgetCategoryIdsFromFirestoreMap(map: Map<String, Any?>): List<String> =
    (map["categoryIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

@Suppress("UNCHECKED_CAST")
fun budgetAccountIdsFromFirestoreMap(map: Map<String, Any?>): List<String> =
    (map["accountIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

fun newBudgetCategoryRelation(budgetId: String, categoryId: String) = BudgetCategoryEntity(
    id = UUID.randomUUID().toString(),
    budgetId = budgetId,
    categoryId = categoryId,
    createdOn = Date(),
    updatedOn = Date(),
)

fun newBudgetAccountRelation(budgetId: String, accountId: String) = BudgetAccountEntity(
    id = UUID.randomUUID().toString(),
    budgetId = budgetId,
    accountId = accountId,
    createdOn = Date(),
    updatedOn = Date(),
)

fun DebtEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "accountId" to accountId,
    "personName" to personName,
    "direction" to direction.name,
    "dueDate" to dueDate?.time,
    "notes" to notes,
    "isSettled" to isSettled,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
)

fun debtEntityFromFirestoreMap(id: String, map: Map<String, Any?>): DebtEntity = DebtEntity(
    id = id,
    accountId = map["accountId"] as? String ?: "",
    personName = map["personName"] as? String ?: "",
    direction = (map["direction"] as? String)?.let { DebtDirection.valueOf(it) } ?: DebtDirection.LENT,
    dueDate = (map["dueDate"] as? Number)?.toLong()?.let { Date(it) },
    notes = map["notes"] as? String ?: "",
    isSettled = map["isSettled"] as? Boolean ?: false,
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
)

fun DebtReminderEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "debtId" to debtId,
    "reminderDate" to reminderDate.time,
    "createdOn" to createdOn.time,
)

fun debtReminderEntityFromFirestoreMap(id: String, map: Map<String, Any?>): DebtReminderEntity = DebtReminderEntity(
    id = id,
    debtId = map["debtId"] as? String ?: "",
    reminderDate = Date((map["reminderDate"] as? Number)?.toLong() ?: 0L),
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
)

fun SavingsGoalEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "accountId" to accountId,
    "name" to name,
    "targetAmount" to targetAmount,
    "targetDate" to targetDate?.time,
    "notes" to notes,
    "isAchieved" to isAchieved,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
)

fun savingsGoalEntityFromFirestoreMap(id: String, map: Map<String, Any?>): SavingsGoalEntity = SavingsGoalEntity(
    id = id,
    accountId = map["accountId"] as? String ?: "",
    name = map["name"] as? String ?: "",
    targetAmount = (map["targetAmount"] as? Number)?.toDouble() ?: 0.0,
    targetDate = (map["targetDate"] as? Number)?.toLong()?.let { Date(it) },
    notes = map["notes"] as? String ?: "",
    isAchieved = map["isAchieved"] as? Boolean ?: false,
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
)

fun RecurringTransactionEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "notes" to notes,
    "categoryId" to categoryId,
    "fromAccountId" to fromAccountId,
    "toAccountId" to toAccountId,
    "type" to type.name,
    "amount" to amount,
    "frequency" to frequency.name,
    "intervalCount" to intervalCount,
    "startDate" to startDate.time,
    "endDate" to endDate?.time,
    "nextOccurrenceDate" to nextOccurrenceDate.time,
    "isActive" to isActive,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
)

fun recurringTransactionEntityFromFirestoreMap(
    id: String,
    map: Map<String, Any?>,
): RecurringTransactionEntity = RecurringTransactionEntity(
    id = id,
    notes = map["notes"] as? String ?: "",
    categoryId = map["categoryId"] as? String ?: "",
    fromAccountId = map["fromAccountId"] as? String ?: "",
    toAccountId = map["toAccountId"] as? String,
    type = (map["type"] as? String)?.let { TransactionType.valueOf(it) } ?: TransactionType.EXPENSE,
    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
    frequency = (map["frequency"] as? String)?.let { RecurrenceFrequency.valueOf(it) }
        ?: RecurrenceFrequency.MONTHLY,
    intervalCount = (map["intervalCount"] as? Number)?.toInt() ?: 1,
    startDate = Date((map["startDate"] as? Number)?.toLong() ?: 0L),
    endDate = (map["endDate"] as? Number)?.toLong()?.let { Date(it) },
    nextOccurrenceDate = Date((map["nextOccurrenceDate"] as? Number)?.toLong() ?: 0L),
    isActive = map["isActive"] as? Boolean ?: true,
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
)

fun ShoppingListEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "categoryId" to categoryId,
    "accountId" to accountId,
    "createdOn" to createdOn.time,
    "updatedOn" to updatedOn.time,
)

fun shoppingListEntityFromFirestoreMap(id: String, map: Map<String, Any?>): ShoppingListEntity =
    ShoppingListEntity(
        id = id,
        name = map["name"] as? String ?: "",
        categoryId = map["categoryId"] as? String ?: "",
        accountId = map["accountId"] as? String ?: "",
        createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
        updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
    )

/** No `updatedOn`/checked flag — a row here always means "still pending" (see
 * `ShoppingListItemEntity`'s doc), so there's nothing else to round-trip besides these fields. */
fun ShoppingListItemEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "shoppingListId" to shoppingListId,
    "name" to name,
    "price" to price,
    "createdOn" to createdOn.time,
)

fun shoppingListItemEntityFromFirestoreMap(
    id: String,
    map: Map<String, Any?>,
): ShoppingListItemEntity = ShoppingListItemEntity(
    id = id,
    shoppingListId = map["shoppingListId"] as? String ?: "",
    name = map["name"] as? String ?: "",
    price = (map["price"] as? Number)?.toDouble() ?: 0.0,
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
)
CLAUDE_FIX_EOF

echo "  core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/cloudbackup/CloudBackupRepositoryImpl.kt"
mkdir -p "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/cloudbackup"
cat > "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/cloudbackup/CloudBackupRepositoryImpl.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.data.cloudbackup

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.BudgetDao
import com.naveenapps.expensemanager.core.database.dao.CategoryDao
import com.naveenapps.expensemanager.core.database.dao.DebtDao
import com.naveenapps.expensemanager.core.database.dao.DebtReminderDao
import com.naveenapps.expensemanager.core.database.dao.RecurringTransactionDao
import com.naveenapps.expensemanager.core.database.dao.SavingsGoalDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListItemDao
import com.naveenapps.expensemanager.core.database.dao.TransactionDao
import com.naveenapps.expensemanager.core.database.entity.BudgetEntity
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * See [CloudBackupRepository]'s doc for why this is structured (one Firestore document per row)
 * rather than a single opaque backup file.
 *
 * IMPORTANT — cannot be verified offline: written without network access, so the Firestore
 * Kotlin API surface below (batch writes, query snapshots) could not be checked against the live
 * SDK docs. Double-check against https://firebase.google.com/docs/firestore/manage-data/add-data
 * once back on a machine with network access.
 *
 * IMPORTANT — Firestore Security Rules (configured in the Firebase console, not in this app's
 * code) must restrict each person's data to themselves, e.g.:
 * ```
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     match /users/{userId}/{document=**} {
 *       allow read, write: if request.auth != null && request.auth.uid == userId;
 *     }
 *   }
 * }
 * ```
 * This project's Firestore is shared with another app (attestation) in the same Firebase
 * project — make sure this rule is scoped under `/users/{userId}/...` specifically and doesn't
 * touch whatever top-level path the other app already uses.
 */
class CloudBackupRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val googleAuthRepository: GoogleAuthRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val debtReminderDao: DebtReminderDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingListItemDao: ShoppingListItemDao,
) : CloudBackupRepository {

    private fun requireUserRoot() =
        googleAuthRepository.getCurrentUserId()?.let { uid ->
            firestore.collection("users").document(uid)
        } ?: throw IllegalStateException("Cannot access cloud backup: not signed in")

    override suspend fun syncAll(): Resource<Boolean> {
        return try {
            val userRoot = requireUserRoot()

            syncCollection(
                collectionRef = userRoot.collection(ACCOUNTS),
                localRows = accountDao.getAllAccountEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(CATEGORIES),
                localRows = categoryDao.getAllValues().orEmpty(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(TRANSACTIONS),
                localRows = transactionDao.getAllTransactionEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(TRANSACTION_SPLIT_ITEMS),
                localRows = transactionDao.getAllSplitItems(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )

            val budgets = budgetDao.getAllBudgetEntities()
            syncCollection(
                collectionRef = userRoot.collection(BUDGETS),
                localRows = budgets,
                idOf = { it.id },
                toMap = { budget ->
                    val categoryIds = budgetDao.getBudgetCategories(budget.id)
                        .orEmpty().map { it.categoryId }
                    val accountIds = budgetDao.getBudgetAccounts(budget.id)
                        .orEmpty().map { it.accountId }
                    budget.toFirestoreMap(categoryIds, accountIds)
                },
            )

            syncCollection(
                collectionRef = userRoot.collection(DEBTS),
                localRows = debtDao.getAllDebtEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(DEBT_REMINDERS),
                localRows = debtReminderDao.getAllDebtReminderEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(SAVINGS_GOALS),
                localRows = savingsGoalDao.getAllSavingsGoalEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(RECURRING_TRANSACTIONS),
                localRows = recurringTransactionDao.getAllRecurringTransactionEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )

            // Shopping lists point at real accounts/categories rather than a hidden one (see
            // ShoppingList's doc), so restoring accounts/categories first, same as everything
            // else here, is enough — no special ordering needed like debt reminders → debts.
            syncCollection(
                collectionRef = userRoot.collection(SHOPPING_LISTS),
                localRows = shoppingListDao.getAllShoppingListEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )
            syncCollection(
                collectionRef = userRoot.collection(SHOPPING_LIST_ITEMS),
                localRows = shoppingListItemDao.getAllShoppingListItemEntities(),
                idOf = { it.id },
                toMap = { it.toFirestoreMap() },
            )

            userRoot.collection(META).document("info")
                .set(mapOf("lastSyncedAt" to Date().time))
                .await()

            Resource.Success(true)
        } catch (exception: Exception) {
            Log.w(TAG, "Cloud sync failed", exception)
            Resource.Error(exception)
        }
    }

    /**
     * Upserts every local row and deletes any remote document that no longer has a matching
     * local row (i.e. was deleted locally since the last sync). Firestore batches are capped at
     * 500 operations, so writes are chunked well under that to leave room for the accompanying
     * deletes in the same batch.
     */
    private suspend fun <T> syncCollection(
        collectionRef: com.google.firebase.firestore.CollectionReference,
        localRows: List<T>,
        idOf: (T) -> String,
        toMap: suspend (T) -> Map<String, Any?>,
    ) {
        val remoteIds = collectionRef.get().await().documents.map { it.id }.toSet()
        val localIds = localRows.map(idOf).toSet()
        val staleRemoteIds = remoteIds - localIds

        val chunkSize = 400
        localRows.chunked(chunkSize).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { row -> batch.set(collectionRef.document(idOf(row)), toMap(row)) }
            batch.commit().await()
        }
        staleRemoteIds.chunked(chunkSize).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { id -> batch.delete(collectionRef.document(id)) }
            batch.commit().await()
        }
    }

    override suspend fun hasRemoteBackup(): Resource<Boolean> {
        return try {
            val metaDoc = requireUserRoot().collection(META).document("info").get().await()
            Resource.Success(metaDoc.exists())
        } catch (exception: Exception) {
            Resource.Success(false)
        }
    }

    override suspend fun restoreAllFromCloud(): Resource<Boolean> {
        return try {
            val userRoot = requireUserRoot()

            userRoot.collection(ACCOUNTS).get().await().documents.forEach { doc ->
                accountDao.insert(accountEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(CATEGORIES).get().await().documents.forEach { doc ->
                categoryDao.insert(categoryEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(TRANSACTIONS).get().await().documents.forEach { doc ->
                transactionDao.insert(transactionEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(TRANSACTION_SPLIT_ITEMS).get().await().documents.forEach { doc ->
                transactionDao.insertSplitItems(
                    listOf(transactionSplitItemEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
                )
            }
            userRoot.collection(BUDGETS).get().await().documents.forEach { doc ->
                val map = doc.data.orEmpty()
                val budget: BudgetEntity = budgetEntityFromFirestoreMap(doc.id, map)
                budgetDao.insertBudget(
                    budgetEntity = budget,
                    categories = budgetCategoryIdsFromFirestoreMap(map),
                    accounts = budgetAccountIdsFromFirestoreMap(map),
                )
            }

            // Debt/savings-goal accounts (AccountType.DEBT/SAVINGS_GOAL) are restored above as
            // part of ACCOUNTS — these just restore their metadata rows, which is why this must
            // stay after the accounts block. Debt reminders reference a debt row, so debts must
            // be restored first.
            userRoot.collection(DEBTS).get().await().documents.forEach { doc ->
                debtDao.insert(debtEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(DEBT_REMINDERS).get().await().documents.forEach { doc ->
                debtReminderDao.insert(debtReminderEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(SAVINGS_GOALS).get().await().documents.forEach { doc ->
                savingsGoalDao.insert(savingsGoalEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(RECURRING_TRANSACTIONS).get().await().documents.forEach { doc ->
                recurringTransactionDao.insert(
                    recurringTransactionEntityFromFirestoreMap(doc.id, doc.data.orEmpty()),
                )
            }

            // Shopping list items reference a shopping list row, so lists must be restored first.
            userRoot.collection(SHOPPING_LISTS).get().await().documents.forEach { doc ->
                shoppingListDao.insert(shoppingListEntityFromFirestoreMap(doc.id, doc.data.orEmpty()))
            }
            userRoot.collection(SHOPPING_LIST_ITEMS).get().await().documents.forEach { doc ->
                shoppingListItemDao.insert(
                    shoppingListItemEntityFromFirestoreMap(doc.id, doc.data.orEmpty()),
                )
            }

            Resource.Success(true)
        } catch (exception: Exception) {
            Log.w(TAG, "Cloud restore failed", exception)
            Resource.Error(exception)
        }
    }

    override suspend fun getLastBackupTime(): Date? {
        return runCatching {
            val metaDoc = requireUserRoot().collection(META).document("info").get().await()
            (metaDoc.get("lastSyncedAt") as? Number)?.toLong()?.let { Date(it) }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "CloudBackupRepository"
        private const val ACCOUNTS = "accounts"
        private const val CATEGORIES = "categories"
        private const val BUDGETS = "budgets"
        private const val TRANSACTIONS = "transactions"
        private const val TRANSACTION_SPLIT_ITEMS = "transaction_split_items"
        private const val DEBTS = "debts"
        private const val DEBT_REMINDERS = "debt_reminders"
        private const val SAVINGS_GOALS = "savings_goals"
        private const val RECURRING_TRANSACTIONS = "recurring_transactions"
        private const val SHOPPING_LISTS = "shopping_lists"
        private const val SHOPPING_LIST_ITEMS = "shopping_list_items"
        private const val META = "meta"
    }
}
CLAUDE_FIX_EOF

echo "  core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di/KoinRepositoryModule.kt"
mkdir -p "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di"
cat > "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/di/KoinRepositoryModule.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.data.di

import com.naveenapps.expensemanager.core.data.repository.AccountRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.AnalyticsRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.BudgetRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.CategoryRepositoryImpl
import com.naveenapps.expensemanager.core.data.cloudbackup.CloudBackupScheduler
import com.naveenapps.expensemanager.core.data.cloudbackup.CloudBackupWorker
import com.naveenapps.expensemanager.core.data.cloudbackup.DatabaseChangeCloudBackupTrigger
import com.naveenapps.expensemanager.core.data.cloudbackup.CloudBackupRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.GoogleAuthRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.RecurringTransactionRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.DebtRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.DebtReminderRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.SavingsGoalRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.ShoppingListRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.ShoppingListItemRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.CountryRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.CurrencyRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.DateRangeFilterRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.DevicePropertyRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.ExportRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.export.CsvExportStrategy
import com.naveenapps.expensemanager.core.data.repository.export.PdfExportStrategy
import com.naveenapps.expensemanager.core.data.repository.FeedbackRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.FirebaseSettingsRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.JsonConverterRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.LocaleRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.ReminderTimeRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.SettingsRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.ShareRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.ThemeRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.TransactionRepositoryImpl
import com.naveenapps.expensemanager.core.data.repository.VersionCheckerRepositoryImpl
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.AnalyticsRepository
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.repository.CategoryRepository
import com.naveenapps.expensemanager.core.repository.CountryRepository
import com.naveenapps.expensemanager.core.repository.CurrencyRepository
import com.naveenapps.expensemanager.core.repository.DateRangeFilterRepository
import com.naveenapps.expensemanager.core.repository.DevicePropertyRepository
import com.naveenapps.expensemanager.core.repository.ExportRepository
import com.naveenapps.expensemanager.core.repository.FeedbackRepository
import com.naveenapps.expensemanager.core.repository.FirebaseSettingsRepository
import com.naveenapps.expensemanager.core.repository.JsonConverterRepository
import com.naveenapps.expensemanager.core.repository.LocaleRepository
import com.naveenapps.expensemanager.core.repository.ReminderTimeRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import com.naveenapps.expensemanager.core.repository.ShareRepository
import com.naveenapps.expensemanager.core.repository.ThemeRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import com.naveenapps.expensemanager.core.repository.VersionCheckerRepository
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository
import com.naveenapps.expensemanager.core.repository.DebtRepository
import com.naveenapps.expensemanager.core.repository.DebtReminderRepository
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val RepositoryModule = module {
    single<ThemeRepository> {
        ThemeRepositoryImpl(
            dataStore = get(),
            versionCheckerRepository = get(),
            dispatchers = get()
        )
    }
    single<LocaleRepository> {
        LocaleRepositoryImpl(
            dataStore = get(),
            dispatchers = get()
        )
    }
    single<AnalyticsRepository> {
        AnalyticsRepositoryImpl()
    }
    single<JsonConverterRepository> {
        JsonConverterRepositoryImpl(
            gson = get(),
            appCoroutineDispatchers = get()
        )
    }
    single<CountryRepository> {
        CountryRepositoryImpl(
            context = androidContext(),
            jsonConverterRepository = get(),
            dispatchers = get()
        )
    }
    single<CurrencyRepository> {
        CurrencyRepositoryImpl(
            dispatchers = get(),
            dataStore = get(),
            numberFormatRepository = get(),
        )
    }
    single<DevicePropertyRepository> { DevicePropertyRepositoryImpl(androidContext()) }
    single<FeedbackRepository> {
        FeedbackRepositoryImpl(
            context = androidContext(),
            feedbackDataStore = get(),
        )
    }
    single<FirebaseSettingsRepository> { FirebaseSettingsRepositoryImpl() }
    single<VersionCheckerRepository> { VersionCheckerRepositoryImpl() }

    single<AccountRepository> {
        AccountRepositoryImpl(
            accountDao = get(),
            transactionDao = get(),
            dispatchers = get()
        )
    }
    single<BudgetRepository> {
        BudgetRepositoryImpl(
            budgetDao = get(),
            dispatchers = get()
        )
    }
    single<DateRangeFilterRepository> {
        DateRangeFilterRepositoryImpl(
            context = androidContext(),
            dataStore = get(),
            dispatcher = get()
        )
    }
    single<ExportRepository> {
        ExportRepositoryImpl(
            dispatchers = get(),
            strategies = listOf(
                CsvExportStrategy(context = androidContext()),
                PdfExportStrategy(context = androidContext()),
            )
        )
    }
    single<ReminderTimeRepository> {
        ReminderTimeRepositoryImpl(
            dataStore = get(),
            dispatchers = get()
        )
    }
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            dataStore = get(),
            dispatchers = get()
        )
    }
    single<ShareRepository> {
        ShareRepositoryImpl(
            context = androidContext(),
            firebaseSettingsRepository = get()
        )
    }
    single<TransactionRepository> {
        TransactionRepositoryImpl(
            transactionDao = get(),
            accountDao = get(),
            categoryDao = get(),
            dispatchers = get()
        )
    }
    single<CategoryRepository> {
        CategoryRepositoryImpl(
            categoryDao = get(),
            transactionDao = get(),
            dispatchers = get()
        )
    }
    single<RecurringTransactionRepository> {
        RecurringTransactionRepositoryImpl(
            recurringTransactionDao = get(),
            accountDao = get(),
            categoryDao = get(),
            dispatchers = get(),
        )
    }
    single<DebtRepository> {
        DebtRepositoryImpl(
            debtDao = get(),
            accountDao = get(),
            dispatchers = get(),
        )
    }
    single<DebtReminderRepository> {
        DebtReminderRepositoryImpl(
            debtReminderDao = get(),
            dispatchers = get(),
        )
    }
    single<SavingsGoalRepository> {
        SavingsGoalRepositoryImpl(
            savingsGoalDao = get(),
            accountDao = get(),
            dispatchers = get(),
        )
    }
    single<ShoppingListRepository> {
        ShoppingListRepositoryImpl(
            shoppingListDao = get(),
            categoryDao = get(),
            accountDao = get(),
            dispatchers = get(),
        )
    }
    single<ShoppingListItemRepository> {
        ShoppingListItemRepositoryImpl(
            shoppingListItemDao = get(),
            dispatchers = get(),
        )
    }

    // Sauvegarde cloud automatique — voir les commentaires de GoogleAuthRepositoryImpl /
    // CloudBackupRepositoryImpl pour le design complet.
    single<GoogleAuthRepository> {
        GoogleAuthRepositoryImpl(
            context = androidContext(),
            firebaseAuth = get(),
        )
    }
    single<CloudBackupRepository> {
        CloudBackupRepositoryImpl(
            firestore = get(),
            googleAuthRepository = get(),
            accountDao = get(),
            categoryDao = get(),
            budgetDao = get(),
            transactionDao = get(),
            debtDao = get(),
            debtReminderDao = get(),
            savingsGoalDao = get(),
            recurringTransactionDao = get(),
            shoppingListDao = get(),
            shoppingListItemDao = get(),
        )
    }
    single {
        CloudBackupScheduler(context = androidContext())
    }
    single {
        DatabaseChangeCloudBackupTrigger(
            database = get(),
            cloudBackupScheduler = get(),
        )
    }
    worker {
        CloudBackupWorker(
            context = androidContext(),
            workerParams = get(),
            googleAuthRepository = get(),
            cloudBackupRepository = get(),
        )
    }
}
CLAUDE_FIX_EOF

echo
echo "Correctif applique. Etapes suivantes :"
echo "  git add -A"
echo "  git commit -m \"Ajout : sauvegarde cloud des listes de courses\""
echo "  git pull --rebase origin main"
echo "  git push"
