package com.naveenapps.expensemanager.core.domain.usecase.category

import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Resource

class CheckCategoryValidationUseCase() {

    operator fun invoke(category: Category): Resource<Boolean> {
        if (category.id.isBlank()) {
            return Resource.Error(Exception("Veuillez spécifier l'identifiant de la catégorie"))
        }

        if (category.storedIcon.backgroundColor.isBlank()) {
            return Resource.Error(Exception("La couleur de fond n'est pas disponible"))
        }

        if (!category.storedIcon.backgroundColor.startsWith("#")) {
            return Resource.Error(Exception("La couleur de fond n'est pas valide"))
        }

        if (category.name.isBlank()) {
            return Resource.Error(Exception("Le nom de la catégorie ne doit pas être vide"))
        }

        return Resource.Success(true)
    }
}
