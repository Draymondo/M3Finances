package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.InstallmentPlanRepository

/**
 * Creates the hidden [AccountType.INSTALLMENT] counterparty account for a new installment
 * plan, then the [InstallmentPlan] metadata row. [installmentPlan].accountId must already be
 * set (by the caller) to the id this use case will give the new hidden account — same
 * ViewModel-generates-the-id convention as `AddDebtUseCase`.
 *
 * No initial movement is recorded: per design a plan starts at 0 already paid, and payments
 * are accumulated later through [AddInstallmentPaymentUseCase]. The hidden account therefore
 * starts at 0 instead of taking an initial transfer.
 */
class AddInstallmentPlanUseCase(
    private val installmentPlanRepository: InstallmentPlanRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(installmentPlan: InstallmentPlan): Resource<Boolean> {
        if (installmentPlan.name.isBlank()) {
            return Resource.Error(Exception("Le nom ne doit pas être vide"))
        }

        if (installmentPlan.targetAmount <= 0.0) {
            return Resource.Error(Exception("Le prix objectif doit être supérieur à 0"))
        }

        val hiddenAccount = Account(
            id = installmentPlan.accountId,
            name = installmentPlan.name,
            type = AccountType.INSTALLMENT,
            storedIcon = StoredIcon(name = "ic_wallet", backgroundColor = "#FF6D00"),
            createdOn = installmentPlan.createdOn,
            updatedOn = installmentPlan.updatedOn,
            amount = 0.0,
        )
        val accountResult = accountRepository.addAccount(hiddenAccount)
        if (accountResult is Resource.Error) {
            return accountResult
        }

        return installmentPlanRepository.addInstallmentPlan(installmentPlan)
    }
}
