package com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * `RecurringTransactionRepository.getAllRecurringTransactions()` doesn't populate the
 * [RecurringTransaction.category] / [RecurringTransaction.fromAccount] / [RecurringTransaction.toAccount]
 * fields (only `findRecurringTransactionById` and `getDueRecurringTransactions` do, see
 * `RecurringTransactionRepositoryImpl`) — this use case fills them in for list display, alongside
 * a currency-formatted amount, the same way `GetBudgetsUseCase` builds its UI model.
 */
class GetRecurringTransactionsUseCase(
    private val repository: RecurringTransactionRepository,
    private val getAllAccountsUseCase: GetAllAccountsUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(): Flow<List<RecurringTransactionUiModel>> {
        return combine(
            repository.getAllRecurringTransactions(),
            getAllAccountsUseCase.invoke(),
            getAllCategoryUseCase.invoke(),
            getCurrencyUseCase.invoke(),
        ) { recurringTransactions, accounts, categories, currency ->
            recurringTransactions.map { recurringTransaction ->
                categories.find { it.id == recurringTransaction.categoryId }?.let {
                    recurringTransaction.category = it
                }
                accounts.find { it.id == recurringTransaction.fromAccountId }?.let {
                    recurringTransaction.fromAccount = it
                }
                recurringTransaction.toAccountId?.let { toAccountId ->
                    recurringTransaction.toAccount = accounts.find { it.id == toAccountId }
                }
                RecurringTransactionUiModel(
                    recurringTransaction = recurringTransaction,
                    formattedAmount = getFormattedAmountUseCase.invoke(
                        recurringTransaction.amount.amount,
                        currency,
                    ),
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
    }
}

@Stable
data class RecurringTransactionUiModel(
    val recurringTransaction: RecurringTransaction,
    val formattedAmount: Amount,
)
