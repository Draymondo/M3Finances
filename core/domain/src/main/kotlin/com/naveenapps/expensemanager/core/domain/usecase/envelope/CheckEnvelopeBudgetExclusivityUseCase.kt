package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository
import kotlinx.coroutines.flow.first

/**
 * Enforces exclusivity between an [com.naveenapps.expensemanager.core.model.Envelope] and a
 * classic [com.naveenapps.expensemanager.core.model.Budget] that specifically targets the same
 * category + period — a general "all categories" budget is a different, coarser concept and is
 * allowed to coexist with per-category envelopes underneath it. Called from both directions —
 * [checkForEnvelope] before creating/updating an envelope, [checkForBudget] before
 * creating/updating a classic budget — so the block applies whichever is created second.
 */
class CheckEnvelopeBudgetExclusivityUseCase(
    private val envelopeRepository: EnvelopeRepository,
    private val budgetRepository: BudgetRepository,
) {

    suspend fun checkForEnvelope(
        categoryId: String,
        selectedMonth: String,
        periodType: BudgetPeriod,
    ): Resource<Boolean> {
        val conflict = budgetRepository.getBudgets().first().any { budget ->
            budget.periodType == periodType &&
                budget.selectedMonth == selectedMonth &&
                !budget.isAllCategoriesSelected &&
                budget.categories.contains(categoryId)
        }
        return if (conflict) {
            Resource.Error(
                Exception(
                    "Un budget classique couvre déjà cette catégorie pour cette période. " +
                        "Une enveloppe et un budget ciblant la même catégorie ne peuvent pas coexister sur la même période.",
                ),
            )
        } else {
            Resource.Success(true)
        }
    }

    suspend fun checkForBudget(
        categories: List<String>,
        isAllCategoriesSelected: Boolean,
        selectedMonth: String,
        periodType: BudgetPeriod,
    ): Resource<Boolean> {
        // A general "all categories" budget is a coarser, coexisting concept — never blocked by
        // per-category envelopes underneath it.
        if (isAllCategoriesSelected) {
            return Resource.Success(true)
        }
        val envelopes = envelopeRepository.findEnvelopesByPeriod(selectedMonth, periodType)
        val conflict = envelopes.any { categories.contains(it.categoryId) }
        return if (conflict) {
            Resource.Error(
                Exception(
                    "Une enveloppe existe déjà pour cette catégorie et cette période. " +
                        "Une enveloppe et un budget ciblant la même catégorie ne peuvent pas coexister sur la même période.",
                ),
            )
        } else {
            Resource.Success(true)
        }
    }
}
