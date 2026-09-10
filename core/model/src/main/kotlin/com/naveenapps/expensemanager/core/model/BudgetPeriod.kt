package com.naveenapps.expensemanager.core.model

/**
 * How a [Budget]'s [Budget.selectedMonth] key should be interpreted and how its spending should
 * be aggregated: against a single day, a calendar week (Monday→Sunday), a calendar month, or an
 * entire calendar year.
 *
 * MONTHLY must stay the first entry (ordinal 0) and YEARLY the second (ordinal 1) — both are
 * persisted as a raw ordinal Int on `BudgetEntity`, and existing rows are backfilled to `0` by
 * migration `MIGRATION_5_6`, so every budget created before this field existed must continue to
 * resolve to MONTHLY. WEEKLY and DAILY were added later and must stay appended after them so
 * already-persisted ordinals never shift.
 */
enum class BudgetPeriod {
    MONTHLY,
    YEARLY,
    WEEKLY,
    DAILY,
}
