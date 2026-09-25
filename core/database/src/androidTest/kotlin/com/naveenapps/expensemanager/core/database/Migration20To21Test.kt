package com.naveenapps.expensemanager.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val OFFLINE_QUEUE_MIGRATION_TEST_DB = "migration_20_21_test"

/** Columns the exported v21 schema declares, in order. */
private val EXPECTED_OFFLINE_QUEUE_COLUMNS = listOf(
    "event_id",
    "package_name",
    "source",
    "channel",
    "title",
    "full_text",
    "post_time",
    "received_at",
    "status",
    "attempt_count",
    "last_error",
)

/** Tables that already existed at version 20 and must survive the migration untouched. */
private val V20_TABLES = listOf(
    "account",
    "budget",
    "category",
    "debt",
    "debt_reminder",
    "envelope",
    "recurring_transaction",
    "savings_goal",
    "shopping_list",
    "shopping_list_items",
    "transaction",
    "transaction_split_items",
    "pending_transaction",
)

/**
 * Chantier 3K : proves that `MIGRATION_20_21` really works on a populated v20 database.
 *
 * The migration is additive (it only creates the offline-queue table), so this test asserts
 * that nothing existing is lost and that the new table matches the exported schema exactly -
 * which `runMigrationsAndValidate(..., validate = true)` enforces, including affinities.
 */
@RunWith(AndroidJUnit4::class)
class Migration20To21Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExpenseManagerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun preservesExistingDataAndCreatesTheOfflineQueueTable() {
        seedV20Database()

        // `validate = true` makes Room compare the migrated schema against 21.json: a wrong
        // affinity (TEXT instead of INTEGER for an ordinal-backed enum) fails right here.
        val db = helper.runMigrationsAndValidate(
            OFFLINE_QUEUE_MIGRATION_TEST_DB,
            21,
            true,
            MIGRATION_20_21,
        )

        // 1. Every v20 table is still present, plus the new one.
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            val tables = buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            V20_TABLES.forEach { table -> assertThat(tables).contains(table) }
            assertThat(tables).contains("pending_notification_processing")
        }

        // 2. Pre-existing data survives, including the 3D identity columns.
        db.query(
            "SELECT `id`, `amount`, `fee`, `merchant`, `source`, `provider_reference`, " +
                "`deduplication_key`, `notification_event_id` FROM `pending_transaction`",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("id"))).isEqualTo("existing-pending")
            assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))).isEqualTo(1250.0)
            assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow("fee"))).isEqualTo(12.5)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("merchant"))).isEqualTo("Existing merchant")
            // TransactionSource is stored by ordinal; WAVE == 0.
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("source"))).isEqualTo(0)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("provider_reference"))).isEqualTo("wave-123")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("deduplication_key"))).isEqualTo("dedup-key-123")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("notification_event_id"))).isEqualTo("event-abc")
        }

        db.query("SELECT `balance` FROM `account` WHERE `id` = 'acc-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getDouble(0)).isEqualTo(27744.0)
        }

        db.query("SELECT `name` FROM `category` WHERE `id` = 'cat-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Food")
        }

        // 3. The new table has the expected columns, affinities, nullability and primary key.
        db.query("PRAGMA table_info(pending_notification_processing)").use { cursor ->
            val columns = mutableListOf<String>()
            val affinities = mutableMapOf<String, String>()
            val notNullColumns = mutableListOf<String>()
            val primaryKeys = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                columns += name
                affinities[name] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                if (cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1) notNullColumns += name
                if (cursor.getInt(cursor.getColumnIndexOrThrow("pk")) > 0) primaryKeys += name
            }

            assertThat(columns).containsExactlyElementsIn(EXPECTED_OFFLINE_QUEUE_COLUMNS).inOrder()
            // Affinities must match the Room converters: ordinal-backed source is INTEGER,
            // name-backed channel/status are TEXT.
            assertThat(affinities["event_id"]).isEqualTo("TEXT")
            assertThat(affinities["source"]).isEqualTo("INTEGER")
            assertThat(affinities["channel"]).isEqualTo("TEXT")
            assertThat(affinities["status"]).isEqualTo("TEXT")
            assertThat(affinities["attempt_count"]).isEqualTo("INTEGER")
            // Only `last_error` is nullable.
            assertThat(notNullColumns).containsExactly(
                "event_id", "package_name", "source", "channel", "title",
                "full_text", "post_time", "received_at", "status", "attempt_count",
            ).inOrder()
            // `event_id` is the primary key: that is what deduplicates the queue for free.
            assertThat(primaryKeys).containsExactly("event_id")
        }

        // 4. The queue starts empty, accepts a row, and rejects a replayed eventId.
        db.query("SELECT COUNT(*) FROM `pending_notification_processing`").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(0)
        }

        val insert = "INSERT INTO `pending_notification_processing` (" +
            "`event_id`, `package_name`, `source`, `channel`, `title`, `full_text`, " +
            "`post_time`, `received_at`, `status`, `attempt_count`, `last_error`" +
            ") VALUES (" +
            "'event-abc', 'com.orange.money', 1, 'PUSH', 'Orange Money', " +
            "'Vous avez recu 5000 FCFA de Raymond', 1720000000000, 1720000000000, 'PENDING', 0, NULL)"
        db.execSQL(insert)
        assertThat(runCatching { db.execSQL(insert) }.isFailure).isTrue()

        db.query("SELECT `full_text`, `source`, `status` FROM `pending_notification_processing`").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Vous avez recu 5000 FCFA de Raymond")
            // ORANGE_MONEY ordinal == 1, confirming the INTEGER affinity round-trips.
            assertThat(cursor.getInt(1)).isEqualTo(1)
            assertThat(cursor.getString(2)).isEqualTo("PENDING")
        }

        db.close()
    }

    private fun seedV20Database() {
        helper.createDatabase(OFFLINE_QUEUE_MIGRATION_TEST_DB, 20).apply {
            execSQL(
                "INSERT INTO `account` (`id`, `name`, `balance`, `currency_code`, `type`, `color`, `sequence`) " +
                    "VALUES ('acc-1', 'Wave', 27744.0, 'XOF', 0, 0, 0)",
            )
            execSQL(
                "INSERT INTO `category` (`id`, `name`, `type`, `icon_id`, `color`, `created_on`, `updated_on`) " +
                    "VALUES ('cat-1', 'Food', 0, 0, 0, 1710000000000, 1710000000000)",
            )
            execSQL(
                "INSERT INTO `pending_transaction` (" +
                    "`id`, `amount`, `fee`, `merchant`, `date`, `transaction_type`, `suggested_category`, " +
                    "`raw_notification`, `source`, `confidence`, `created_on`, `scheduled_date`, `account_id`, " +
                    "`category_id`, `provider_reference`, `deduplication_key`, `channel`, `notification_event_id`" +
                    ") VALUES (" +
                    "'existing-pending', 1250.0, 12.5, 'Existing merchant', 1720000000000, 0, 'Food', " +
                    "'Original notification', 0, 0.9, 1710000000000, 1730000000000, 'acc-1', 'cat-1', " +
                    "'wave-123', 'dedup-key-123', 'PUSH', 'event-abc')",
            )
            close()
        }
    }
}
