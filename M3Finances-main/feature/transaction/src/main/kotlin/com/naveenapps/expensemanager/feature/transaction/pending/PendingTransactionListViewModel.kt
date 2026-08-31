package com.naveenapps.expensemanager.feature.transaction.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetPendingTransactionsUseCase
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PendingTransactionListViewModel(
    getPendingTransactionsUseCase: GetPendingTransactionsUseCase,
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val appComposeNavigator: AppComposeNavigator
) : ViewModel() {

    private val _pendingTransactions = MutableStateFlow<List<PendingTransaction>>(emptyList())
    val pendingTransactions = _pendingTransactions.asStateFlow()

    init {
        getPendingTransactionsUseCase.invoke()
            .onEach { list ->
                _pendingTransactions.update { list }
            }.launchIn(viewModelScope)
    }

    fun openTransactionCreate(transaction: PendingTransaction) {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(id = null, pendingTransactionId = transaction.id))
    }

    fun dismissTransaction(id: String) {
        viewModelScope.launch {
            pendingTransactionRepository.deletePendingTransaction(id)
        }
    }
    
    fun closePage() {
        appComposeNavigator.popBackStack()
    }
}
