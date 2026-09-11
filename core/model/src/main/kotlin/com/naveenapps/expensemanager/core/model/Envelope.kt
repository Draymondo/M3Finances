package com.naveenapps.expensemanager.core.model

import java.util.Date

/**
 * A sum allocated to a single category, tracked only in the dedicated "Enveloppe" screen
 * (Paramètres > Outils) — never surfaced on the Dashboard nor in the list of classic [Budget]s.
 * A separate concept from [Budget], not a filtered view over it.
 *
 * [categoryId] + [selectedMonth] (for a given [periodType]) is exclusive with any classic
 * [Budget] covering the same category and period — see `CheckEnvelopeExclusivityUseCase` in
 * core/domain, which extends the existing `CheckBudgetValidateUseCase` rule in both directions.
 *
 * Reuses [Budget]'s period-key convention: which format [selectedMonth] holds is determined by
 * [periodType] — see [Budget.selectedMonth] for the exact per-period format.
 */
data class Envelope(
    val id: String,
    val categoryId: String,
    val name: String? = null,
    val amount: Double = 0.0,
    val selectedMonth: String,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val createdOn: Date,
    val updatedOn: Date,
)
