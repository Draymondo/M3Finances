package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.domain.usecase.envelope.CheckEnvelopeBudgetExclusivityUseCase
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.BudgetRepository

class UpdateBudgetUseCase(
    private val repository: BudgetRepository,
    private val checkBudgetValidateUseCase: CheckBudgetValidateUseCase,
    private val checkEnvelopeBudgetExclusivityUseCase: CheckEnvelopeBudgetExclusivityUseCase,
) {

    suspend operator fun invoke(budget: Budget): Resource<Boolean> {
        return when (val validationResult = checkBudgetValidateUseCase(budget)) {
            is Resource.Error -> {
                validationResult
            }

            is Resource.Success -> {
                when (
                    val exclusivityResult = checkEnvelopeBudgetExclusivityUseCase.checkForBudget(
                        categories = budget.categories,
                        isAllCategoriesSelected = budget.isAllCategoriesSelected,
                        selectedMonth = budget.selectedMonth,
                        periodType = budget.periodType,
                    )
                ) {
                    is Resource.Error -> exclusivityResult
                    is Resource.Success -> repository.updateBudget(budget)
                }
            }
        }
    }
}
