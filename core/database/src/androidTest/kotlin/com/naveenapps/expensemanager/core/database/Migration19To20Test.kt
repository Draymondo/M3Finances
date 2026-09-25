package com.naveenapps.expensemanager.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val PENDING_TRANSACTION_MIGRATION_TEST_DB = "migration_19_20_test"

@RunWith(AndroidJUnit4::class)
class Migration19To20Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExpenseManagerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun preservesExistingPendingTransactionAndAddsNullableIdentityColumns() {
        helper.createDatabase(PENDING_TRANSACTION_MIGRATION_TEST_DB, 19).apply {
            execSQL(
                """
                INSERT INTO `pending_transaction` (
                    `id`, `amount`, `fee`, `merchant`, `date`,
                    `transaction_type`, `suggested_category`, `raw_notification`,
                    `source`, `confidence`, `created_on`, `scheduled_date`,
                    `account_id`, `category_id`
                ) VALUES (
                    'existing-pending', 1250.0, 12.5, 'Existing merchant',
                    1720000000000, 0, 'Food', 'Original notification',
                    0, 0.9, 1710000000000, 1730000000000,
                    'existing-account', 'existing-category'
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            PENDING_TRANSACTION_MIGRATION_TEST_DB,
            20,
            true,
            MIGRATION_19_20,
        )

        db.query(
            """
            SELECT `id`, `amount`, `fee`, `merchant`, `date`,
                   `transaction_type`, `suggested_category`, `raw_notification`,
                   `source`, `confidence`, `created_on`, `scheduled_date`,
                   `account_id`, `category_id`, `provider_reference`, `deduplication_key`
            FROM `pending_transaction`
            """.trimIndent(),
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()

            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("id")))
                .isEqualTo("existing-pending")
            assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))).isEqualTo(1250.0)
            assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow("fee"))).isEqualTo(12.5)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("merchant")))
                .isEqualTo("Existing merchant")
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("date")))
                .isEqualTo(1720000000000L)
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("created_on")))
                .isEqualTo(1710000000000L)
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_date")))
                .isEqualTo(1730000000000L)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("account_id")))
                .isEqualTo("existing-account")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("category_id")))
                .isEqualTo("existing-category")
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("provider_reference"))).isTrue()
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("deduplication_key"))).isTrue()
        }

        db.query("PRAGMA table_info(pending_transaction)").use { cursor ->
            val nullableColumns = buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (name == "provider_reference" || name == "deduplication_key") {
                        add(name to cursor.getInt(cursor.getColumnIndexOrThrow("notnull")))
                    }
                }
            }

            assertThat(nullableColumns).containsExactly(
                "provider_reference" to 0,
                "deduplication_key" to 0,
            ).inOrder()
        }

        db.close()
    }
}
