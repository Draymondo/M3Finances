package com.naveenapps.expensemanager.core.database.utils

import androidx.room.TypeConverter
import com.naveenapps.expensemanager.core.model.TransactionSource

object TransactionSourceConverter {

    @TypeConverter
    fun ordinalToTransactionSource(value: Int?): TransactionSource? {
        return value?.let { TransactionSource.entries[it] }
    }

    @TypeConverter
    fun transactionSourceToOrdinal(source: TransactionSource?): Int? {
        return source?.ordinal
    }
}