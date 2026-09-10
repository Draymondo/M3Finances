package com.naveenapps.expensemanager.core.domain.usecase.account

import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.Resource

class CheckAccountValidationUseCase() {

    operator fun invoke(account: Account): Resource<Boolean> {
        if (account.id.isBlank()) {
            return Resource.Error(Exception("L'identifiant ne doit pas être vide"))
        }

        if (account.storedIcon.name.isBlank()) {
            return Resource.Error(Exception("Le nom de l'icône ne doit pas être vide"))
        }

        if (account.storedIcon.backgroundColor.isBlank()) {
            return Resource.Error(Exception("La couleur de fond ne doit pas être vide"))
        }

        if (!account.storedIcon.backgroundColor.startsWith("#")) {
            return Resource.Error(Exception("La couleur de fond n'est pas valide"))
        }

        if (account.name.isBlank()) {
            return Resource.Error(Exception("Le nom du compte ne doit pas être vide"))
        }

        return Resource.Success(true)
    }
}
