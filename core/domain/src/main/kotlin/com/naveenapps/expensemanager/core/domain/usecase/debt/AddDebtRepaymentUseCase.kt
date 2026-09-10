package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.isLent
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID

/**
 * Records a partial (or full) repayment against an existing [Debt] as a transfer between the
 * real account and the debt's hidden counterparty account — the exact reverse direction of the
 * original lend/borrow transfer recorded by [AddDebtUseCase], so the debt account's balance
 * moves back toward zero either way.
 */
class AddDebtRepaymentUseCase(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
) {
    suspend operator fun invoke(
        debt: Debt,
        amount: Double,
        realAccountId: String,
        notes: String,
    ): Resource<Boolean> {
        if (amount <= 0.0) {
            return Resource.Error(Exception("Le montant doit être supérieur à 0"))
        }

        val fallbackCategoryId = getAllCategoryUseCase.invoke().first().firstOrNull()?.id
            ?: return Resource.Error(Exception("Aucune catégorie disponible"))

        val (fromAccountId, toAccountId) = if (debt.direction.isLent()) {
            debt.accountId to realAccountId
        } else {
            realAccountId to debt.accountId
        }

        return addTransactionUseCase.invoke(
            Transaction(
                id = UUID.randomUUID().toString(),
                notes = notes,
                categoryId = fallbackCategoryId,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = Amount(amount),
                imagePath = "",
                type = TransactionType.TRANSFER,
                createdOn = Date(),
                updatedOn = Date(),
            ),
        )
    }
}
