package com.naveenapps.expensemanager.core.model

/**
 * IMPORTANT: persisted by ordinal (see `RecurrenceFrequencyConverter`), so new entries must
 * always be appended at the end — same caveat as `AccountType`.
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}
