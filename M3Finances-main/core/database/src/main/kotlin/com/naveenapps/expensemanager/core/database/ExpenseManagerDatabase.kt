package com.naveenapps.expensemanager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.naveenapps.expensemanager.core.database.dao.AccountDao
import com.naveenapps.expensemanager.core.database.dao.BudgetDao
import com.naveenapps.expensemanager.core.database.dao.CategoryDao
import com.naveenapps.expensemanager.core.database.dao.DebtDao
import com.naveenapps.expensemanager.core.database.dao.DebtReminderDao
import com.naveenapps.expensemanager.core.database.dao.SavingsGoalDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListDao
import com.naveenapps.expensemanager.core.database.dao.ShoppingListItemDao
import com.naveenapps.expensemanager.core.database.dao.RecurringTransactionDao
import com.naveenapps.expensemanager.core.database.dao.TransactionDao
import com.naveenapps.expensemanager.core.database.entity.AccountEntity
import com.naveenapps.expensemanager.core.database.entity.BudgetAccountEntity
import com.naveenapps.expensemanager.core.database.entity.BudgetCategoryEntity
import com.naveenapps.expensemanager.core.database.entity.BudgetEntity
import com.naveenapps.expensemanager.core.database.entity.CategoryEntity
import com.naveenapps.expensemanager.core.database.entity.DebtEntity
import com.naveenapps.expensemanager.core.database.entity.DebtReminderEntity
import com.naveenapps.expensemanager.core.database.entity.SavingsGoalEntity
import com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity
import com.naveenapps.expensemanager.core.database.entity.ShoppingListItemEntity
import com.naveenapps.expensemanager.core.database.entity.RecurringTransactionEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionSplitItemEntity
import com.naveenapps.expensemanager.core.database.utils.AccountTypeConverter
import com.naveenapps.expensemanager.core.database.utils.CategoryTypeConverter
import com.naveenapps.expensemanager.core.database.utils.DateConverter
import com.naveenapps.expensemanager.core.database.utils.DebtDirectionConverter
import com.naveenapps.expensemanager.core.database.utils.RecurrenceFrequencyConverter
import com.naveenapps.expensemanager.core.database.utils.TransactionTypeConverter

/**
 * File name of the Room database on disk, relative to `Context.getDatabasePath(...)`. Public so
 * that features that need the raw file — local manual backup/restore, and the automatic cloud
 * backup — can locate it without duplicating this string (Room also creates `-wal`/`-shm`
 * sidecar files alongside it while the database is open; see CloudBackupRepositoryImpl for how
 * those are handled before copying).
 */
const val DATABASE_FILE_NAME = "expense_manager_database.db"

/**
 * The Room database for this app
 */
@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        BudgetCategoryEntity::class,
        BudgetAccountEntity::class,
        RecurringTransactionEntity::class,
        DebtEntity::class,
        DebtReminderEntity::class,
        SavingsGoalEntity::class,
        TransactionSplitItemEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        com.naveenapps.expensemanager.core.database.entity.PendingTransactionEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
@TypeConverters(
    DateConverter::class,
    TransactionTypeConverter::class,
    CategoryTypeConverter::class,
    AccountTypeConverter::class,
    RecurrenceFrequencyConverter::class,
    DebtDirectionConverter::class,
)
abstract class ExpenseManagerDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun accountDao(): AccountDao

    abstract fun budgetDao(): BudgetDao

    abstract fun recurringTransactionDao(): RecurringTransactionDao

    abstract fun debtDao(): DebtDao

    abstract fun debtReminderDao(): DebtReminderDao

    abstract fun savingsGoalDao(): SavingsGoalDao

    abstract fun shoppingListDao(): ShoppingListDao

    abstract fun shoppingListItemDao(): ShoppingListItemDao
    
    abstract fun pendingTransactionDao(): com.naveenapps.expensemanager.core.database.dao.PendingTransactionDao
}
