package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.naveenapps.expensemanager.core.model.DebtDirection
import java.util.Date

@Entity(
    tableName = "debt",
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
data class DebtEntity(
    @androidx.room.PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "person_name")
    val personName: String,
    @ColumnInfo(name = "direction")
    val direction: DebtDirection,
    @ColumnInfo(name = "due_date")
    val dueDate: Date?,
    @ColumnInfo(name = "notes")
    val notes: String,
    @ColumnInfo(name = "is_settled", defaultValue = "0")
    val isSettled: Boolean = false,
    @ColumnInfo(name = "created_on")
    val createdOn: Date,
    @ColumnInfo(name = "updated_on")
    val updatedOn: Date,
)
