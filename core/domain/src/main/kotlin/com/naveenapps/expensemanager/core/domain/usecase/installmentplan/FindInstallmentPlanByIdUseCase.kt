package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.InstallmentPlanRepository

class FindInstallmentPlanByIdUseCase(
    private val installmentPlanRepository: InstallmentPlanRepository,
) {
    suspend operator fun invoke(id: String): Resource<InstallmentPlan> {
        return installmentPlanRepository.findInstallmentPlanById(id)
    }
}
