package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Thin CRUD over the `debt` metadata table only. Creating/deleting a debt also involves the
 * hidden counterparty account and the transfer transaction(s) that record money movement — that
 * cross-repository orchestration lives in the domain use cases (see `AddDebtUseCase`,
 * `DeleteDebtUseCase`), not here, matching how `ProcessDueRecurringTransactionsUseCase` composes
 * `AddTransactionUseCase` rather than duplicating transaction-creation logic in a repository.
 */
interface DebtRepository {

    /** Enriched with [Debt.account] already populated (unlike the recurring-transaction
     * equivalent, which required a separate domain-layer fix after this same gap was found). */
    fun getDebts(): Flow<List<Debt>>

    suspend fun findDebtById(id: String): Resource<Debt>

    suspend fun addDebt(debt: Debt): Resource<Boolean>

    suspend fun updateDebt(debt: Debt): Resource<Boolean>
}
