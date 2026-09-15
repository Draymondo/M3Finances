package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Pour l'écran dédié "Transactions prévues" (Paramètres > Outils) uniquement — remonte toutes
 * les transactions prévues, à venir comme déjà dues. Ne pas utiliser pour le widget de
 * notifications de l'accueil : voir [GetPendingTransactionsUseCase] pour ça.
 */
class GetScheduledPendingTransactionsUseCase(
    private val repository: PendingTransactionRepository,
) {
    operator fun invoke(): Flow<List<PendingTransaction>> {
        return repository.getAllScheduledPendingTransactions()
    }
}
