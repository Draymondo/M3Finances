package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Date

/**
 * Reads the payment history of one plan: every transfer involving its hidden account, most
 * recent first. One-shot (not a Flow) — callers refresh it after recording a new payment.
 * [accountId] is the plan's hidden account id.
 */
class GetInstallmentPlanPaymentsUseCase(
    private val transactionRepository: TransactionRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
) {
    suspend operator fun invoke(accountId: String): List<InstallmentPaymentUiModel> {
        val currency = getCurrencyUseCase.invoke().first()
        return transactionRepository.getTransactionsByAccountId(accountId)
            .sortedByDescending { it.createdOn }
            .map { transaction ->
                InstallmentPaymentUiModel(
                    id = transaction.id,
                    date = transaction.createdOn,
                    amount = getFormattedAmountUseCase.invoke(transaction.amount.amount, currency),
                    isRefund = transaction.fromAccountId == accountId,
                    notes = transaction.notes,
                )
            }
    }
}

@Stable
data class InstallmentPaymentUiModel(
    val id: String,
    val date: Date,
    val amount: Amount,
    /** True when money flowed back out of the plan's hidden account into a real account. */
    val isRefund: Boolean,
    val notes: String,
)
