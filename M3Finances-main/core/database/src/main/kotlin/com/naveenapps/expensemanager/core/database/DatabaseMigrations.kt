package com.naveenapps.expensemanager.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `account` ADD COLUMN `sequence` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget_new` (" +
                "`id` TEXT NOT NULL, " +
                "`selected_month` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`all_accounts_selected` INTEGER NOT NULL, " +
                "`all_categories_selected` INTEGER NOT NULL, " +
                "`created_on` INTEGER NOT NULL, " +
                "`updated_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `budget_new` " +
                "(`id`, `selected_month`, `amount`, `all_accounts_selected`, `all_categories_selected`, `created_on`, `updated_on`) " +
                "SELECT `id`, `selected_month`, `amount`, `all_accounts_selected`, `all_categories_selected`, `created_on`, `updated_on` " +
                "FROM `budget`"
        )
        db.execSQL("DROP TABLE `budget`")
        db.execSQL("ALTER TABLE `budget_new` RENAME TO `budget`")
    }
}

/**
 * Adds a nullable `default_category_key` column used to mark which rows are the app's
 * built-in seeded categories (as opposed to user-created ones), so their display name can be
 * localized via string resources instead of the raw stored `name`.
 *
 * For existing installs, the 11 categories seeded by `PreloadDatabaseInitializer.BASE_CATEGORY_LIST`
 * always got the deterministic ids "1".."11" on first launch (that list is only ever inserted into
 * an empty table). We backfill the key for those ids, but only when `name` still matches the
 * original seeded English name — if a user has since renamed one of these categories, we leave
 * `default_category_key` null so their custom name is preserved instead of being overwritten by a
 * translation the user never asked for.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `category` ADD COLUMN `default_category_key` TEXT DEFAULT NULL")

        val defaults = listOf(
            Triple("1", "Clothing", "clothing"),
            Triple("2", "Entertainment", "entertainment"),
            Triple("3", "Food", "food"),
            Triple("4", "Health", "health"),
            Triple("5", "Leisure", "leisure"),
            Triple("6", "Shopping", "shopping"),
            Triple("7", "Transportation", "transportation"),
            Triple("8", "Utilities", "utilities"),
            Triple("9", "Salary", "salary"),
            Triple("10", "Gift", "gift"),
            Triple("11", "Coupons", "coupons"),
        )

        defaults.forEach { (id, originalName, key) ->
            db.execSQL(
                "UPDATE `category` SET `default_category_key` = ? " +
                    "WHERE `id` = ? AND `name` = ?",
                arrayOf(key, id, originalName),
            )
        }
    }
}

/**
 * Adds a `period_type` column to `budget` so a budget can cover either a single month or an
 * entire year (see `core.model.BudgetPeriod`). `BudgetPeriod.MONTHLY` is ordinal 0, matching the
 * column's `DEFAULT 0`, so every budget that existed before this migration — all of which were
 * implicitly monthly — is correctly backfilled without needing a separate UPDATE statement.
 */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `budget` ADD COLUMN `period_type` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_transaction` (" +
                "`id` TEXT NOT NULL, " +
                "`notes` TEXT NOT NULL, " +
                "`category_id` TEXT NOT NULL, " +
                "`from_account_id` TEXT NOT NULL, " +
                "`to_account_id` TEXT, " +
                "`type` INTEGER NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`frequency` INTEGER NOT NULL, " +
                "`interval_count` INTEGER NOT NULL DEFAULT 1, " +
                "`start_date` INTEGER NOT NULL, " +
                "`end_date` INTEGER, " +
                "`next_occurrence_date` INTEGER NOT NULL, " +
                "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                "`created_on` INTEGER NOT NULL, " +
                "`updated_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`from_account_id`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`to_account_id`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

/**
 * Adds the `debt` table. Each row is metadata for a debt with a person; the actual amount owed
 * lives on the balance of a hidden `AccountType.DEBT` account (see `account_id`), created
 * alongside this row and never surfaced in the normal account picker or dashboard net balance.
 * Lending/borrowing and every partial repayment are recorded as ordinary TRANSFER transactions
 * against that account, reusing the existing atomic balance mechanism instead of a parallel
 * ledger — see the `Debt` model for the full design rationale.
 */
internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt` (" +
                "`id` TEXT NOT NULL, " +
                "`account_id` TEXT NOT NULL, " +
                "`person_name` TEXT NOT NULL, " +
                "`direction` INTEGER NOT NULL, " +
                "`due_date` INTEGER, " +
                "`notes` TEXT NOT NULL, " +
                "`is_settled` INTEGER NOT NULL DEFAULT 0, " +
                "`created_on` INTEGER NOT NULL, " +
                "`updated_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

/**
 * Adds the `debt_reminder` table — zero or more user-chosen reminder dates per debt (see
 * `DebtReminderEntity`), separate from the debt's own single `due_date`.
 */
internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_reminder` (" +
                "`id` TEXT NOT NULL, " +
                "`debt_id` TEXT NOT NULL, " +
                "`reminder_date` INTEGER NOT NULL, " +
                "`created_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`debt_id`) REFERENCES `debt`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

/**
 * Adds the `savings_goal` table — a goal's amount saved so far lives on the balance of a hidden
 * `AccountType.SAVINGS_GOAL` account (see `account_id`), created the same way `MIGRATION_7_8`
 * created the `debt` table for `AccountType.DEBT`. Contributions are recorded as ordinary
 * TRANSFER transactions against that account — see the `SavingsGoal` model for the full design
 * rationale.
 */
internal val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `savings_goal` (" +
                "`id` TEXT NOT NULL, " +
                "`account_id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`target_amount` REAL NOT NULL, " +
                "`target_date` INTEGER, " +
                "`notes` TEXT NOT NULL, " +
                "`is_achieved` INTEGER NOT NULL DEFAULT 0, " +
                "`created_on` INTEGER NOT NULL, " +
                "`updated_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

internal val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transaction_split_item` (" +
                "`id` TEXT NOT NULL, " +
                "`transaction_id` TEXT NOT NULL, " +
                "`category_id` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`notes` TEXT, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`transaction_id`) REFERENCES `transaction`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

/**
 * Adds the `shopping_list` and `shopping_list_item` tables. Unlike `debt`/`savings_goal`, a
 * shopping list points straight at a real `category`/`account` — no hidden counterparty account
 * is created. An item row always means "still pending"; checking one off deletes its row and
 * creates an ordinary expense transaction instead of flipping a checked flag — see the
 * `ShoppingList`/`ShoppingListItem` models for the full design rationale.
 */
internal val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shopping_list` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`category_id` TEXT NOT NULL, " +
                "`account_id` TEXT NOT NULL, " +
                "`created_on` INTEGER NOT NULL, " +
                "`updated_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shopping_list_item` (" +
                "`id` TEXT NOT NULL, " +
                "`shopping_list_id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`price` REAL NOT NULL, " +
                "`created_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`shopping_list_id`) REFERENCES `shopping_list`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_transaction` (" +
                "`id` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`fee` REAL, " +
                "`merchant` TEXT, " +
                "`date` INTEGER NOT NULL, " +
                "`transaction_type` INTEGER NOT NULL, " +
                "`suggested_category` TEXT, " +
                "`raw_notification` TEXT, " +
                "`created_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `pending_transaction` ADD COLUMN `source` INTEGER NOT NULL DEFAULT 7"
        )
        db.execSQL(
            "ALTER TABLE `pending_transaction` ADD COLUMN `confidence` REAL"
        )
    }
}

internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `savings_goal` ADD COLUMN `savings_strategy` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `savings_goal` ADD COLUMN `target_percentage` REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE `savings_goal` ADD COLUMN `estimated_completion_date` INTEGER DEFAULT NULL")
    }
}

internal val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `budget` ADD COLUMN `goal_type` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `budget` ADD COLUMN `name` TEXT DEFAULT NULL")
    }
}

