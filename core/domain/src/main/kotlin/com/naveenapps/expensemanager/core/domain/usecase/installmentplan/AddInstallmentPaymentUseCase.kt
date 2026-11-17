package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID

/**
 * Records a payment toward (or, when [isRefund] is true, a reimbursement from) an existing
 * [InstallmentPlan] as a transfer between the real account and the plan's hidden counterparty
 * account — same shape as `AddSavingsGoalContributionUseCase`.
 */
class AddInstallmentPaymentUseCase(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
) {
    suspend operator fun invoke(
        installmentPlan: InstallmentPlan,
        amount: Double,
        realAccountId: String,
        isRefund: Boolean,
        notes: String,
    ): Resource<Boolean> {
        if (amount <= 0.0) {
            return Resource.Error(Exception("Le montant doit être supérieur à 0"))
        }

        // Any category works here — transfers aren't shown by category in the UI, this only
        // exists to satisfy AddTransactionUseCase's non-blank categoryId requirement.
        val fallbackCategoryId = getAllCategoryUseCase.invoke().first().firstOrNull()?.id
            ?: return Resource.Error(Exception("Aucune catégorie disponible"))

        val (fromAccountId, toAccountId) = if (isRefund) {
            installmentPlan.accountId to realAccountId
        } else {
            realAccountId to installmentPlan.accountId
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
