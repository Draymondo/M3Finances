package com.naveenapps.expensemanager.feature.dashboard

import kotlin.math.abs

/**
 * Calculates whether the active income budget amount is aligned with the calculated required income
 * within a given tolerance (default ±1%).
 */
fun isRequiredIncomeAligned(
    requiredAmount: Double,
    budgetAmount: Double?,
    tolerance: Double = 0.01,
): Boolean {
    if (requiredAmount <= 0.0) return true
    if (budgetAmount == null || budgetAmount <= 0.0) return false

    val diff = abs(budgetAmount - requiredAmount)
    return diff <= requiredAmount * tolerance
}

