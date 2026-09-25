package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import java.util.Date
import kotlinx.coroutines.flow.Flow

interface PendingTransactionRepository {

    fun getAllPendingTransactions(): Flow<List<PendingTransaction>>

    /** Toutes les transactions prévues, à venir comme déjà dues — pour l'écran dédié
     * "Transactions prévues" (Paramètres > Outils) uniquement. Ne pas utiliser pour le
     * widget de notifications de l'accueil : [getAllPendingTransactions] reste la seule
     * source "actionable maintenant" pour ça. */
    fun getAllScheduledPendingTransactions(): Flow<List<PendingTransaction>>

    fun getPendingTransactionById(id: String): Flow<PendingTransaction?>

    /**
     * Tente d'ajouter une transaction en attente.
     *
     * `Success(true)` signifie que l'opération a été acceptée : la ligne a été insérée ou
     * une déduplication connue a été reconnue. `Success(false)` reste un résultat idempotent
     * accepté par les appelants. `Error` signifie qu'aucune persistance n'a pu être confirmée.
     */
    suspend fun addPendingTransaction(transaction: PendingTransaction): Resource<Boolean>

    suspend fun deletePendingTransaction(id: String): Resource<Boolean>

    suspend fun deleteAllPendingTransactions(): Resource<Boolean>

    suspend fun updateScheduledDate(id: String, newDate: Date): Resource<Boolean>

    suspend fun countScheduledDue(): Int
}

