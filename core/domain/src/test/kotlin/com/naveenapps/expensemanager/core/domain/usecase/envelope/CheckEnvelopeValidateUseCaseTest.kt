package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.testing.FAKE_ENVELOPE
import org.junit.Test

class CheckEnvelopeValidateUseCaseTest {

    private val checkEnvelopeValidateUseCase = CheckEnvelopeValidateUseCase()

    @Test
    fun whenEnvelopeIsValidShouldReturnSuccess() {
        val response = checkEnvelopeValidateUseCase.invoke(FAKE_ENVELOPE)
        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
    }

    @Test
    fun whenEnvelopeIdIsBlankShouldReturnError() {
        val response = checkEnvelopeValidateUseCase.invoke(FAKE_ENVELOPE.copy(id = ""))
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenEnvelopeCategoryIsBlankShouldReturnError() {
        val response = checkEnvelopeValidateUseCase.invoke(FAKE_ENVELOPE.copy(categoryId = ""))
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenEnvelopeAmountIsZeroShouldReturnError() {
        val response = checkEnvelopeValidateUseCase.invoke(FAKE_ENVELOPE.copy(amount = 0.0))
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }
}
