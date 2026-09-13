package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.common.utils.getEndOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheMonth
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

/**
 * Calcule le revenu total du mois calendaire courant pour les catégories sélectionnées.
 *
 * Pattern exact de [GetEnvelopeTransactionsUseCase], adapté pour :
 * - `transactionType = TransactionType.INCOME`
 * - Fenêtre = mois courant (getStartOfTheMonth / getEndOfTheMonth sur Date())
 * - Toutes les catégories passées en paramètre (autoDetectCategoryIds)
 * - Tous les comptes (aucun filtre de compte)
 *
 * @return [Resource.Success] avec la somme des montants INCOME, ou [Resource.Error].
 */
class GetAutoDetectedIncomeUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(categoryIds: List<String>): Resource<Double> {
        return try {
            val accounts = accountRepository.getAccounts().firstOrNull()?.map { it.id }
                ?: emptyList()

            val now = Date()
            val startDate = now.getStartOfTheMonth()
            val endDate = now.getEndOfTheMonth()

            val transactions = transactionRepository.getFilteredTransaction(
                accounts = accounts,
                categories = categoryIds,
                transactionType = listOf(TransactionType.INCOME.ordinal),
                startDate = startDate,
                endDate = endDate,
            ).firstOrNull() ?: emptyList()

            val total = transactions.sumOf { it.amount.amount }
            Resource.Success(total)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }
}

