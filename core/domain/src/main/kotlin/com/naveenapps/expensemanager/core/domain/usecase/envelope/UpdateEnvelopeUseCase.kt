package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository

class UpdateEnvelopeUseCase(
    private val repository: EnvelopeRepository,
    private val checkEnvelopeValidateUseCase: CheckEnvelopeValidateUseCase,
    private val checkEnvelopeBudgetExclusivityUseCase: CheckEnvelopeBudgetExclusivityUseCase,
) {

    suspend operator fun invoke(envelope: Envelope): Resource<Boolean> {
        return when (val validationResult = checkEnvelopeValidateUseCase(envelope)) {
            is Resource.Error -> validationResult
            is Resource.Success -> {
                val duplicate = repository.findEnvelopesByCategoryAndPeriod(
                    categoryId = envelope.categoryId,
                    selectedMonth = envelope.selectedMonth,
                    periodType = envelope.periodType,
                ).any { it.id != envelope.id }
                if (duplicate) {
                    return Resource.Error(
                        Exception("Une autre enveloppe existe déjà pour cette catégorie sur cette période"),
                    )
                }
                when (
                    val exclusivityResult = checkEnvelopeBudgetExclusivityUseCase.checkForEnvelope(
                        categoryId = envelope.categoryId,
                        selectedMonth = envelope.selectedMonth,
                        periodType = envelope.periodType,
                    )
                ) {
                    is Resource.Error -> exclusivityResult
                    is Resource.Success -> repository.updateEnvelope(envelope)
                }
            }
        }
    }
}
