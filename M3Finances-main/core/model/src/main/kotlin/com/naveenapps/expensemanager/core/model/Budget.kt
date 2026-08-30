package com.naveenapps.expensemanager.core.model

import java.util.Date

data class Budget(
    val id: String,
    val amount: Double = 0.0,
    /**
     * Round-trip key identifying the period this budget covers. Which format applies is
     * determined by [periodType]: a month+year key (`Date.toMonthAndYearKey`) for
     * [BudgetPeriod.MONTHLY], a year-only key (`Date.toYear`) for [BudgetPeriod.YEARLY], a
     * day key (`Date.toDayKey`) for [BudgetPeriod.DAILY], or that same day-key format applied to
     * the Monday of the covered week (`Date.toWeekKey`) for [BudgetPeriod.WEEKLY].
     */
    val selectedMonth: String,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categories: List<String>,
    val accounts: List<String>,
    val isAllAccountsSelected: Boolean,
    val isAllCategoriesSelected: Boolean,
    val createdOn: Date,
    val updatedOn: Date,
)
