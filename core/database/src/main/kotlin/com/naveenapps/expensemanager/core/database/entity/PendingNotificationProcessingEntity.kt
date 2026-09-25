package com.naveenapps.expensemanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.naveenapps.expensemanager.core.model.PendingNotificationProcessing
import com.naveenapps.expensemanager.core.model.PendingNotificationStatus
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource

/**
 * File persistante des notifications en attente d'un traitement Gemini.
 *
 * La clé primaire est l'`eventId` du chantier 3D : c'est ce qui rend la file naturellement
 * dédupliquée, sans introduire le moindre algorithme de déduplication supplémentaire.
 */
@Entity(tableName = "pending_notification_processing")
data class PendingNotificationProcessingEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "source")
    val source: TransactionSource,
    @ColumnInfo(name = "channel")
    val channel: TransactionChannel,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "full_text")
    val fullText: String,
    @ColumnInfo(name = "post_time")
    val postTime: Long,
    @ColumnInfo(name = "received_at")
    val receivedAt: Long,
    @ColumnInfo(name = "status")
    val status: PendingNotificationStatus,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int,
    @ColumnInfo(name = "last_error")
    val lastError: String?,
)

fun PendingNotificationProcessingEntity.toModel(): PendingNotificationProcessing =
    PendingNotificationProcessing(
        eventId = eventId,
        packageName = packageName,
        source = source,
        channel = channel,
        title = title,
        fullText = fullText,
        postTime = postTime,
        receivedAt = receivedAt,
        status = status,
        attemptCount = attemptCount,
        lastError = lastError,
    )

fun PendingNotificationProcessing.toEntityModel(): PendingNotificationProcessingEntity =
    PendingNotificationProcessingEntity(
        eventId = eventId,
        packageName = packageName,
        source = source,
        channel = channel,
        title = title,
        fullText = fullText,
        postTime = postTime,
        receivedAt = receivedAt,
        status = status,
        attemptCount = attemptCount,
        lastError = lastError,
    )
