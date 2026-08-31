package com.naveenapps.expensemanager.feature.transaction.list

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.getAmountTextColor
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionGroup
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update


import kotlinx.coroutines.launch
import com.naveenapps.expensemanager.core.domain.usecase.transaction.DeleteTransactionUseCase

class TransactionListViewModel(
    getCurrencyUseCase: GetCurrencyUseCase,
    getFormattedAmountUseCase: GetFormattedAmountUseCase,
    getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _transactions = MutableStateFlow(TransactionListState(emptyList()))
    val state = _transactions.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()

    init {
        combine(
            getCurrencyUseCase.invoke(),
            getTransactionWithFilterUseCase.invoke(),
        ) { currency, transactions ->
            allTransactions = transactions ?: emptyList()

            val groupedItem = transactions?.groupBy {
                it.createdOn.toCompleteDateWithDate()
            }?.map {
                val totalAmount = it.value.toTransactionSum()
                TransactionGroup(
                    date = it.key,
                    amountTextColor = totalAmount.getAmountTextColor(),
                    totalAmount = getFormattedAmountUseCase.invoke(totalAmount, currency),
                    transactions = it.value.map { transaction ->
                        transaction.toTransactionUIModel(
                            getFormattedAmountUseCase.invoke(
                                transaction.amount.amount,
                                currency,
                            ),
                        )
                    },
                )
            }

            _transactions.update {
                it.copy(
                    transactionListItem = groupedItem?.convertGroupToTransactionListItems()
                        ?: emptyList()
                )
            }
        }.flowOn(appCoroutineDispatchers.computation).launchIn(viewModelScope)
    }

    private fun openCreateScreen(transactionId: String? = null) {
        appComposeNavigator.navigate(
            ExpenseManagerScreens.TransactionCreate(transactionId),
        )
    }

    private fun openSearchScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionSearch)
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun toggleSelection(transactionId: String) {
        _transactions.update { state ->
            val currentSelected = state.selectedTransactions
            val newSelected = if (currentSelected.contains(transactionId)) {
                currentSelected - transactionId
            } else {
                currentSelected + transactionId
            }
            state.copy(
                selectedTransactions = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    private fun clearSelection() {
        _transactions.update {
            it.copy(selectedTransactions = emptySet(), isSelectionMode = false)
        }
    }

    private fun deleteSelected() {
        val selectedIds = _transactions.value.selectedTransactions
        if (selectedIds.isEmpty()) return

        val transactionsToDelete = allTransactions.filter { selectedIds.contains(it.id) }

        viewModelScope.launch(appCoroutineDispatchers.io) {
            transactionsToDelete.forEach { transaction ->
                deleteTransactionUseCase.invoke(transaction)
            }
            clearSelection()
        }
    }

    fun processAction(action: TransactionListAction) {
        when (action) {
            TransactionListAction.ClosePage -> closePage()
            TransactionListAction.OpenCreateTransaction -> openCreateScreen()
            TransactionListAction.OpenSearch -> openSearchScreen()
            is TransactionListAction.OpenEdiTransaction -> {
                if (_transactions.value.isSelectionMode) {
                    toggleSelection(action.transactionId)
                } else {
                    openCreateScreen(action.transactionId)
                }
            }
            is TransactionListAction.ToggleSelection -> toggleSelection(action.transactionId)
            TransactionListAction.ClearSelection -> clearSelection()
            TransactionListAction.DeleteSelected -> deleteSelected()
        }
    }
}

fun List<Transaction>.toTransactionSum() =
    this.sumOf {
        when (it.type) {
            TransactionType.INCOME -> {
                it.amount.amount
            }

            TransactionType.EXPENSE -> {
                it.amount.amount * -1
            }

            TransactionType.TRANSFER -> {
                0.0
            }
        }
    }

fun List<TransactionGroup>.convertGroupToTransactionListItems(): List<TransactionListItem> {
    return buildList {
        this@convertGroupToTransactionListItems.forEach {
            add(
                TransactionListItem.HeaderItem(
                    date = it.date,
                    amountTextColor = it.amountTextColor,
                    totalAmount = it.totalAmount.amountString ?: ""
                )
            )

            it.transactions.forEach {
                add(TransactionListItem.TransactionItem(date = it))
            }

            add(TransactionListItem.Divider)
        }
    }
}

sealed class TransactionListItem {

    data class HeaderItem(
        val date: String,
        val amountTextColor: Int,
        val totalAmount: String,
    ) : TransactionListItem()

    data class TransactionItem(
        val date: TransactionUiItem,
    ) : TransactionListItem()

    data object Divider : TransactionListItem()
}
