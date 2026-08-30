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

fun shoppingListEntityFromFirestoreMap(
    id: String,
    map: Map<String, Any?>,
): ShoppingListEntity = ShoppingListEntity(
    id = id,
    name = map["name"] as? String ?: "",
    categoryId = map["categoryId"] as? String ?: "",
    accountId = map["accountId"] as? String ?: "",
    createdOn = Date((map["createdOn"] as? Number)?.toLong() ?: 0L),
    updatedOn = Date((map["updatedOn"] as? Number)?.toLong() ?: 0L),
)

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
