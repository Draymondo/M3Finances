package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.util.Date

/**
 * A single user-chosen reminder date for a [DebtEntity]. Unlike the recurring-transaction or
 * daily-summary reminders (one fixed schedule each), a debt can have any number of these — the
 * person picks whichever dates matter to them (e.g. "a week before", "on payday") rather than
 * the app deriving them from the due date automatically. See `DebtReminderScheduler` for how
 * each row becomes a one-time WorkManager job.
 */
@Entity(
    tableName = "debt_reminder",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("debt_id"),
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DebtReminderEntity(
    @androidx.room.PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "debt_id")
    val debtId: String,
    @ColumnInfo(name = "reminder_date")
    val reminderDate: Date,
    @ColumnInfo(name = "created_on")
    val createdOn: Date,
)
