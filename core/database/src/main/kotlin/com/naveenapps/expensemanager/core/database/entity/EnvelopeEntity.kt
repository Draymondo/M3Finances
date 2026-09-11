package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "envelope",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("category_id"),
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EnvelopeEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "name")
    val name: String? = null,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "selected_month")
    val selectedMonth: String,
    /** Raw ordinal of `core.model.BudgetPeriod` (0 = MONTHLY, 1 = YEARLY, 2 = WEEKLY, 3 = DAILY). */
    @ColumnInfo(name = "period_type", defaultValue = "0")
    val periodType: Int = 0,
    @ColumnInfo(name = "created_on")
    val createdOn: Date,
    @ColumnInfo(name = "updated_on")
    val updatedOn: Date,
)
