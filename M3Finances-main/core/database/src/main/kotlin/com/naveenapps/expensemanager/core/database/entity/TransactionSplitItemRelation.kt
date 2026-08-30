package com.naveenapps.expensemanager.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionSplitItemRelation(
    @Embedded val splitItemEntity: TransactionSplitItemEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id",
    )
    val categoryEntity: CategoryEntity?,
)
