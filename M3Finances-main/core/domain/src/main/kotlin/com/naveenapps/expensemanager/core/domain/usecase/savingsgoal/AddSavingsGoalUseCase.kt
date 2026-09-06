package com.naveenapps.expensemanager.core.domain.usecase.savingsgoal

import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Records a new savings goal: creates its hidden [AccountType.SAVINGS_GOAL] account, optionally
 * an initial contribution transfer from [realAccountId], then the [SavingsGoal] metadata row.
 * [savingsGoal].accountId must already be set (by the caller) to the id this use case will give
 * the new hidden account — same ViewModel-generates-the-id convention as `AddDebtUseCase`.
 *
 * [realAccountId] is nullable, same reasoning as `AddDebtUseCase`: pass a real account to record
 * an actual transfer for the starting contribution; pass null (with [initialAmount] left at 0)
 * to start the goal empty.
 */
class AddSavingsGoalUseCase(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val accountRepository: AccountRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
) {
    suspend operator fun invoke(
        savingsGoal: SavingsGoal,
        initialAmount: Double,
        realAccountId: String?,
    ): Resource<Boolean> {
        if (savingsGoal.name.isBlank()) {
            return Resource.Error(Exception("Name shouldn't be blank"))
        }

        if (savingsGoal.targetAmount <= 0.0) {
            return Resource.Error(Exception("Target amount should be greater than 0"))
        }

        if (savingsGoal.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME) {
            val pct = savingsGoal.targetPercentage
            if (pct == null || pct <= 0.0 || pct > 100.0) {
                return Resource.Error(Exception("Target percentage must be between 0 and 100"))
            }
        }

        if (initialAmount < 0.0) {
            return Resource.Error(Exception("Initial amount can't be negative"))
        }

        val hiddenAccount = Account(
            id = savingsGoal.accountId,
            name = savingsGoal.name,
            type = AccountType.SAVINGS_GOAL,
            storedIcon = StoredIcon(name = "ic_wallet", backgroundColor = "#00BFA5"),
            createdOn = savingsGoal.createdOn,
            updatedOn = savingsGoal.updatedOn,
            // No real account is involved, so there's no transfer to set the balance below —
            // give the hidden account its starting balance directly instead (mirrors AddDebtUseCase).
            amount = if (realAccountId == null) initialAmount else 0.0,
        )
        val accountResult = accountRepository.addAccount(hiddenAccount)
        if (accountResult is Resource.Error) {
            return accountResult
        }

        if (realAccountId != null && initialAmount > 0.0) {
            // Any category works here — transfers aren't shown by category in the UI, this only
            // exists to satisfy AddTransactionUseCase's non-blank categoryId requirement.
            val fallbackCategoryId = getAllCategoryUseCase.invoke().first().firstOrNull()?.id
                ?: return Resource.Error(Exception("No category available"))

            val transactionResult = addTransactionUseCase.invoke(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    notes = savingsGoal.notes,
                    categoryId = fallbackCategoryId,
                    fromAccountId = realAccountId,
                    toAccountId = savingsGoal.accountId,
                    amount = Amount(initialAmount),
                    imagePath = "",
                    type = TransactionType.TRANSFER,
                    createdOn = savingsGoal.createdOn,
                    updatedOn = savingsGoal.updatedOn,
                ),
            )
            if (transactionResult is Resource.Error) {
                return transactionResult
            }
        }

        return savingsGoalRepository.addSavingsGoal(savingsGoal)
    }
}
