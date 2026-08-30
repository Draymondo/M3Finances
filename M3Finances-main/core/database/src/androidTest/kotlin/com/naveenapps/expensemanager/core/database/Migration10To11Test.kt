package com.naveenapps.expensemanager.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val SPLIT_TEST_DB = "migration_10_11_test"

@RunWith(AndroidJUnit4::class)
class Migration10To11Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExpenseManagerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun createsSplitItemTableWithCascadeForeignKeys() {
        helper.createDatabase(SPLIT_TEST_DB, 10).close()

        val db = helper.runMigrationsAndValidate(SPLIT_TEST_DB, 11, true, MIGRATION_10_11)
        val columns = db.query("PRAGMA table_info(transaction_split_item)")
        val columnNames = buildList {
            while (columns.moveToNext()) {
                add(columns.getString(columns.getColumnIndexOrThrow("name")))
            }
        }
        columns.close()

        assertThat(columnNames).containsExactly(
            "id",
            "transaction_id",
            "category_id",
            "amount",
            "notes",
        ).inOrder()

        val foreignKeys = db.query("PRAGMA foreign_key_list(transaction_split_item)")
        val deleteActions = buildList {
            while (foreignKeys.moveToNext()) {
                add(foreignKeys.getString(foreignKeys.getColumnIndexOrThrow("on_delete")))
            }
        }
        foreignKeys.close()
        db.close()

        assertThat(deleteActions).containsExactly("CASCADE", "CASCADE")
    }
}
