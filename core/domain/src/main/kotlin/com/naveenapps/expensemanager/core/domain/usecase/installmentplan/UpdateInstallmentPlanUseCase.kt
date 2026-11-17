package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.InstallmentPlanRepository

/** Updates a plan's metadata (name, target amount, deadline, notes, or the manual
 * [InstallmentPlan.isCompleted] flag). Doesn't touch the hidden account or any transaction —
 * use [AddInstallmentPaymentUseCase] for that. */
class UpdateInstallmentPlanUseCase(
    private val installmentPlanRepository: InstallmentPlanRepository,
) {
    suspend operator fun invoke(installmentPlan: InstallmentPlan): Resource<Boolean> {
        if (installmentPlan.name.isBlank()) {
            return Resource.Error(Exception("Le nom ne doit pas être vide"))
        }

        if (installmentPlan.targetAmount <= 0.0) {
            return Resource.Error(Exception("Le prix objectif doit être supérieur à 0"))
        }

        return installmentPlanRepository.updateInstallmentPlan(installmentPlan)
    }
}
