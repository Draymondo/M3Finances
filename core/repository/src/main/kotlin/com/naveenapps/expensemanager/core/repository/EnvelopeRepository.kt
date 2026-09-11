package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface EnvelopeRepository {

    fun getEnvelopes(): Flow<List<Envelope>>

    suspend fun findEnvelopeById(id: String): Resource<Envelope>

    /** Envelopes already covering [categoryId] for the given [selectedMonth]/[periodType] key —
     * used both by the exclusivity check against classic budgets and to prevent duplicate
     * envelopes on the same category + period. */
    suspend fun findEnvelopesByCategoryAndPeriod(
        categoryId: String,
        selectedMonth: String,
        periodType: com.naveenapps.expensemanager.core.model.BudgetPeriod,
    ): List<Envelope>

    /** All envelopes active for a period key, regardless of category — sum of [Envelope.amount]
     * is the virtual general-budget fallback used when no classic "all categories" budget
     * exists for that period. */
    suspend fun findEnvelopesByPeriod(
        selectedMonth: String,
        periodType: com.naveenapps.expensemanager.core.model.BudgetPeriod,
    ): List<Envelope>

    suspend fun addEnvelope(envelope: Envelope): Resource<Boolean>

    suspend fun updateEnvelope(envelope: Envelope): Resource<Boolean>

    suspend fun deleteEnvelope(envelope: Envelope): Resource<Boolean>
}
