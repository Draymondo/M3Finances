package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "savings_goal",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("account_id"),
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SavingsGoalEntity(
    @androidx.room.PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,
    @ColumnInfo(name = "target_date")
    val targetDate: Date?,
    @ColumnInfo(name = "notes")
    val notes: String,
    @ColumnInfo(name = "is_achieved", defaultValue = "0")
    val isAchieved: Boolean = false,
    @ColumnInfo(name = "created_on")
    val createdOn: Date,
    @ColumnInfo(name = "updated_on")
    val updatedOn: Date,
    @ColumnInfo(name = "savings_strategy", defaultValue = "0")
    val savingsStrategy: Int = 0,
    @ColumnInfo(name = "target_percentage")
    val targetPercentage: Double? = null,
    @ColumnInfo(name = "estimated_completion_date")
    val estimatedCompletionDate: Date? = null,
)
