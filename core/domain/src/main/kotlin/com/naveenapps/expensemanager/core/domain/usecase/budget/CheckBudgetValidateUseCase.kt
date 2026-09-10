package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.Resource

class CheckBudgetValidateUseCase {

    operator fun invoke(budget: Budget): Resource<Boolean> {
        if (budget.id.isBlank()) {
            return Resource.Error(Exception("Veuillez spécifier l'identifiant du budget"))
        }

        if (budget.isAllAccountsSelected.not() && budget.accounts.isEmpty()) {
            return Resource.Error(Exception("Veuillez sélectionner au moins un compte"))
        }

        if (budget.isAllCategoriesSelected.not() && budget.categories.isEmpty()) {
            return Resource.Error(Exception("Veuillez sélectionner au moins une catégorie"))
        }

        if (budget.amount <= 0.0) {
            return Resource.Error(Exception("Le montant ne doit pas être nul"))
        }

        return Resource.Success(true)
    }
}
