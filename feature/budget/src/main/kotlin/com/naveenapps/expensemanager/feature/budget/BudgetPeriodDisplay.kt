package com.naveenapps.expensemanager.feature.budget

import androidx.annotation.StringRes
import com.naveenapps.expensemanager.core.model.BudgetPeriod

/** Shared between the create and detail screens so both equivalence cards label each row the
 * same way. */
@StringRes
fun periodLabelRes(periodType: BudgetPeriod): Int = when (periodType) {
    BudgetPeriod.YEARLY -> R.string.budget_equivalent_yearly
    BudgetPeriod.MONTHLY -> R.string.budget_equivalent_monthly
    BudgetPeriod.WEEKLY -> R.string.budget_equivalent_weekly
    BudgetPeriod.DAILY -> R.string.budget_equivalent_daily
}
