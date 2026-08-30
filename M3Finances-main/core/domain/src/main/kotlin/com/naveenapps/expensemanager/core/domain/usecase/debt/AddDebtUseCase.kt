package com.naveenapps.expensemanager.core.domain.usecase.debt

import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.isLent
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.DebtRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Records a new debt: creates its hidden [AccountType.DEBT] counterparty account, optionally the
 * initial lend/borrow transfer between that account and [realAccountId], then the [Debt]
 * metadata row. [debt].accountId must already be set (by the caller) to the id this use case
 * will give the new hidden account — see `RecurringTransactionCreateViewModel` for the same
 * ViewModel-generates-the-id convention.
 *
 * Direction sets which way the balance moves: LENT makes the debt account's balance positive
 * (amount owed to the user); BORROWED makes it negative (amount the user owes). Repayments later
 * move money the opposite way — see `AddDebtRepaymentUseCase`.
 *
 * [realAccountId] is nullable: pass a real account when the debt corresponds to money actually
 * advanced from (or received into) one of the user's own accounts — a transfer is recorded and
 * that account's balance moves accordingly. Pass null when no such movement ever happened — e.g.
 * money owed for work or services rendered, never paid out of any tracked account — in which
 * case no transfer is created and the hidden account is simply created with the signed amount
 * as its starting balance.
 *
 * These steps aren't wrapped in a single database transaction (no use case in this codebase
 * currently does that across repositories — e.g. `ProcessDueRecurringTransactionsUseCase` has
 * the same non-atomic shape), so a failure partway through can leave an orphaned account. That's
 * an acceptable, consistent tradeoff for now rather than introducing a new pattern.
 */
class AddDebtUseCase(
    private val debtRepository: DebtRepository,
    private val accountRepository: AccountRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCategoryUseCase: GetAllCategoryUseCase,
) {
    suspend operator fun invoke(debt: Debt, initialAmount: Double, realAccountId: String?): Resource<Boolean> {
        if (debt.personName.isBlank()) {
            return Resource.Error(Exception("Person name shouldn't be blank"))
        }

        if (initialAmount <= 0.0) {
            return Resource.Error(Exception("Amount should be greater than 0"))
        }

        val hiddenAccount = Account(
            id = debt.accountId,
            name = debt.personName,
            type = AccountType.DEBT,
            storedIcon = StoredIcon(name = "ic_wallet", backgroundColor = "#7C4DFF"),
            createdOn = debt.createdOn,
            updatedOn = debt.updatedOn,
            // No real account is involved, so there's no transfer to set the balance below —
            // give the hidden account its starting balance directly instead. Signed the same
            // way the transfer path would leave it: positive when LENT, negative when BORROWED.
            amount = if (realAccountId == null) {
                if (debt.direction.isLent()) initialAmount else -initialAmount
            } else {
                0.0
            },
        )
        val accountResult = accountRepository.addAccount(hiddenAccount)
        if (accountResult is Resource.Error) {
            return accountResult
        }

        if (realAccountId != null) {
            // Any category works here — transfers aren't shown by category in the UI, this only
            // exists to satisfy AddTransactionUseCase's non-blank categoryId requirement,
            // matching how the regular transaction-create screen already picks an arbitrary
            // category for transfers.
            val fallbackCategoryId = getAllCategoryUseCase.invoke().first().firstOrNull()?.id
                ?: return Resource.Error(Exception("No category available"))

            val (fromAccountId, toAccountId) = if (debt.direction.isLent()) {
                realAccountId to debt.accountId
            } else {
                debt.accountId to realAccountId
            }

            val transactionResult = addTransactionUseCase.invoke(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    notes = debt.notes,
                    categoryId = fallbackCategoryId,
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = Amount(initialAmount),
                    imagePath = "",
                    type = TransactionType.TRANSFER,
                    createdOn = debt.createdOn,
                    updatedOn = debt.updatedOn,
                ),
            )
            if (transactionResult is Resource.Error) {
                return transactionResult
            }
        }

        return debtRepository.addDebt(debt)
    }
}
