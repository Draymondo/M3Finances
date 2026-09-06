package com.naveenapps.expensemanager.core.database.di

import androidx.room.Room
import com.naveenapps.expensemanager.core.database.DATABASE_FILE_NAME
import com.naveenapps.expensemanager.core.database.ExpenseManagerDatabase
import com.naveenapps.expensemanager.core.database.MIGRATION_2_3
import com.naveenapps.expensemanager.core.database.MIGRATION_3_4
import com.naveenapps.expensemanager.core.database.MIGRATION_4_5
import com.naveenapps.expensemanager.core.database.MIGRATION_5_6
import com.naveenapps.expensemanager.core.database.MIGRATION_6_7
import com.naveenapps.expensemanager.core.database.MIGRATION_7_8
import com.naveenapps.expensemanager.core.database.MIGRATION_8_9
import com.naveenapps.expensemanager.core.database.MIGRATION_9_10
import com.naveenapps.expensemanager.core.database.MIGRATION_10_11
import com.naveenapps.expensemanager.core.database.MIGRATION_11_12
import com.naveenapps.expensemanager.core.database.MIGRATION_12_13
import com.naveenapps.expensemanager.core.database.MIGRATION_13_14
import com.naveenapps.expensemanager.core.database.MIGRATION_14_15
import com.naveenapps.expensemanager.core.database.MIGRATION_15_16
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DatabaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ExpenseManagerDatabase::class.java,
            DATABASE_FILE_NAME,
        ).addMigrations(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
        ).build()
    }
    single { get<ExpenseManagerDatabase>().categoryDao() }
    single { get<ExpenseManagerDatabase>().accountDao() }
    single { get<ExpenseManagerDatabase>().transactionDao() }
    single { get<ExpenseManagerDatabase>().budgetDao() }
    single { get<ExpenseManagerDatabase>().recurringTransactionDao() }
    single { get<ExpenseManagerDatabase>().debtDao() }
    single { get<ExpenseManagerDatabase>().debtReminderDao() }
    single { get<ExpenseManagerDatabase>().savingsGoalDao() }
    single { get<ExpenseManagerDatabase>().shoppingListDao() }
    single { get<ExpenseManagerDatabase>().shoppingListItemDao() }
    single { get<ExpenseManagerDatabase>().pendingTransactionDao() }
}
