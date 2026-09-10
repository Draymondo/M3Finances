package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID

/**
 * Records a contribution to (or, when [isWithdrawal] is true, a withdrawal from) an existing
 * [SavingsGoal] as a transfer between the real account and the goal's hidden counterparty
 * account — same shape as `AddDebtRepaymentUseCase`, just with two directions instead of one
 * fixed by [com.naveenapps.expensemanager.core.model.DebtDirection] (a goal has no borrow/lend
 * split to derive it from).
 */
class AddSavingsGoalContributionUseCase(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
) {
    suspend operator fun invoke(
        savingsGoal: SavingsGoal,
        amount: Double,
        realAccountId: String,
        isWithdrawal: Boolean,
        notes: String,
    ): Resource<Boolean> {
        if (amount <= 0.0) {
            return Resource.Error(Exception("Le montant doit être supérieur à 0"))
        }

        val fallbackCategoryId = getAllCategoryUseCase.invoke().first().firstOrNull()?.id
            ?: return Resource.Error(Exception("Aucune catégorie disponible"))

        val (fromAccountId, toAccountId) = if (isWithdrawal) {
            savingsGoal.accountId to realAccountId
        } else {
            realAccountId to savingsGoal.accountId
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
