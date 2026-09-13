package com.naveenapps.expensemanager.core.model

enum class ToolType(val key: String) {
    RECURRING_TRANSACTIONS("recurring_transactions"),
    DEBTS("debts"),
    SAVINGS_GOALS("savings_goals"),
    SHOPPING_LISTS("shopping_lists"),
    SCHEDULED_TRANSACTIONS("scheduled_transactions"),
    ENVELOPES("envelopes"),
    WORK_TIME("work_time");

    companion object {
        fun fromKey(key: String): ToolType? = entries.find { it.key == key }
    }
}
