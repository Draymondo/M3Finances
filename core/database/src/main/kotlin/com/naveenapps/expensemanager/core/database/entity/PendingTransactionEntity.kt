package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.TransactionSource

@Entity(
    tableName = "pending_transaction",
    indices = [
        Index(value = ["source", "provider_reference"], unique = true),
        Index(value = ["deduplication_key"]),
        Index(value = ["notification_event_id"], unique = true),
    ],
)
data class PendingTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "fee")
    val fee: Double?,
    @ColumnInfo(name = "merchant")
    val merchant: String?,
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "transaction_type")
    val transactionType: TransactionType,
    @ColumnInfo(name = "suggested_category")
    val suggestedCategory: String?,
    @ColumnInfo(name = "raw_notification")
    val rawNotification: String?,
    @ColumnInfo(name = "source")
    val source: TransactionSource,
    @ColumnInfo(name = "confidence")
    val confidence: Float?,
    @ColumnInfo(name = "created_on")
    val createdOn: Long,
    @ColumnInfo(name = "scheduled_date")
    val scheduledDate: Long? = null,
    @ColumnInfo(name = "account_id")
    val accountId: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    @ColumnInfo(name = "provider_reference")
    val providerReference: String? = null,
    @ColumnInfo(name = "deduplication_key")
    val deduplicationKey: String? = null,
    @ColumnInfo(name = "channel")
    val channel: TransactionChannel? = null,
    @ColumnInfo(name = "notification_event_id")
    val notificationEventId: String? = null,
)

