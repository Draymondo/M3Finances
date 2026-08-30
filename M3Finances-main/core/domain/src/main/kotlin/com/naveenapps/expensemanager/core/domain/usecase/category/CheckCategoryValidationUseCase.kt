package com.naveenapps.expensemanager.core.domain.usecase.category

import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Resource

class CheckCategoryValidationUseCase() {

    operator fun invoke(category: Category): Resource<Boolean> {
        if (category.id.isBlank()) {
            return Resource.Error(Exception("Please specify the category id"))
        }

        if (category.storedIcon.backgroundColor.isBlank()) {
            return Resource.Error(Exception("Background color is not available"))
        }

        if (!category.storedIcon.backgroundColor.startsWith("#")) {
            return Resource.Error(Exception("Background color is not valid"))
        }

        if (category.name.isBlank()) {
            return Resource.Error(Exception("Category name shouldn't be blank"))
        }

        return Resource.Success(true)
    }
}
