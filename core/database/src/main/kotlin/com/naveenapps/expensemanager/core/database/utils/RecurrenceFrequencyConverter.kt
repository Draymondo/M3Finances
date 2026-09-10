package com.naveenapps.expensemanager.core.database.utils

import androidx.room.TypeConverter
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency

object RecurrenceFrequencyConverter {

    @TypeConverter
    fun ordinalToRecurrenceFrequency(value: Int?): RecurrenceFrequency? {
        return value?.let { RecurrenceFrequency.entries[it] }
    }

    @TypeConverter
    fun recurrenceFrequencyToOrdinal(frequency: RecurrenceFrequency?): Int? {
        return frequency?.ordinal
    }
}
