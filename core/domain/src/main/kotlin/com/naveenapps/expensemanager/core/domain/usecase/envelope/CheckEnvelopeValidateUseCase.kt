package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource

class CheckEnvelopeValidateUseCase {

    operator fun invoke(envelope: Envelope): Resource<Boolean> {
        if (envelope.id.isBlank()) {
            return Resource.Error(Exception("Veuillez spécifier l'identifiant de l'enveloppe"))
        }

        if (envelope.categoryId.isBlank()) {
            return Resource.Error(Exception("Veuillez sélectionner une catégorie"))
        }

        if (envelope.amount <= 0.0) {
            return Resource.Error(Exception("Le montant ne doit pas être nul"))
        }

        return Resource.Success(true)
    }
}
