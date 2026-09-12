package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository

class DeleteEnvelopeUseCase(
    private val repository: EnvelopeRepository,
) {

    suspend operator fun invoke(envelope: Envelope): Resource<Boolean> {
        if (envelope.id.isBlank()) {
            return Resource.Error(Exception("Veuillez spécifier l'identifiant de l'enveloppe"))
        }
        return repository.deleteEnvelope(envelope)
    }
}
