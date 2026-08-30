package com.naveenapps.expensemanager.core.database.utils

import androidx.room.TypeConverter
import com.naveenapps.expensemanager.core.model.DebtDirection

object DebtDirectionConverter {

    @TypeConverter
    fun ordinalToDebtDirection(value: Int?): DebtDirection? {
        return value?.let { DebtDirection.entries[it] }
    }

    @TypeConverter
    fun debtDirectionToOrdinal(direction: DebtDirection?): Int? {
        return direction?.ordinal
    }
}
