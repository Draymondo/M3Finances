package com.naveenapps.expensemanager.core.database.utils

import androidx.room.TypeConverter
import com.naveenapps.expensemanager.core.model.TransactionChannel

object TransactionChannelConverter {

    @TypeConverter
    fun stringToTransactionChannel(value: String?): TransactionChannel? {
        return value?.let {
            runCatching { TransactionChannel.valueOf(it) }.getOrNull()
        }
    }

    @TypeConverter
    fun transactionChannelToString(channel: TransactionChannel?): String? {
        return channel?.name
    }
}

