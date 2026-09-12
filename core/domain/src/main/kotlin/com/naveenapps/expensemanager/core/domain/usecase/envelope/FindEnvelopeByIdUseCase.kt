package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository

class FindEnvelopeByIdUseCase(private val repository: EnvelopeRepository) {

    suspend operator fun invoke(envelopeId: String?): Resource<Envelope> {
        if (envelopeId.isNullOrBlank()) {
            return Resource.Error(Exception("Identifiant d'enveloppe invalide"))
        }
        return repository.findEnvelopeById(envelopeId)
    }
}
