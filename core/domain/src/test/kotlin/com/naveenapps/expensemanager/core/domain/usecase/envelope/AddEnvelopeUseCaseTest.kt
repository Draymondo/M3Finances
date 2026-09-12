package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import com.naveenapps.expensemanager.core.testing.FAKE_ENVELOPE
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AddEnvelopeUseCaseTest : BaseCoroutineTest() {

    private val envelopeRepository: EnvelopeRepository = mock()
    private val budgetRepository: BudgetRepository = mock()
    private val checkEnvelopeValidateUseCase = CheckEnvelopeValidateUseCase()
    private val checkEnvelopeBudgetExclusivityUseCase =
        CheckEnvelopeBudgetExclusivityUseCase(envelopeRepository, budgetRepository)
    private lateinit var addEnvelopeUseCase: AddEnvelopeUseCase

    override fun onCreate() {
        super.onCreate()
        addEnvelopeUseCase = AddEnvelopeUseCase(
            envelopeRepository,
            checkEnvelopeValidateUseCase,
            checkEnvelopeBudgetExclusivityUseCase,
        )
    }

    @Test
    fun whenEnvelopeIsValidShouldAddSuccessfully() = runTest {
        whenever(envelopeRepository.findEnvelopesByCategoryAndPeriod(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(budgetRepository.getBudgets()).thenReturn(flowOf(emptyList()))
        whenever(envelopeRepository.addEnvelope(FAKE_ENVELOPE)).thenReturn(Resource.Success(true))

        val response = addEnvelopeUseCase.invoke(FAKE_ENVELOPE)
        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
    }

    @Test
    fun whenEnvelopeIsInvalidShouldReturnError() = runTest {
        val response = addEnvelopeUseCase.invoke(FAKE_ENVELOPE.copy(id = ""))
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenDuplicateEnvelopeExistsForCategoryAndPeriodShouldReturnError() = runTest {
        whenever(envelopeRepository.findEnvelopesByCategoryAndPeriod(any(), any(), any()))
            .thenReturn(listOf(FAKE_ENVELOPE.copy(id = "2")))

        val response = addEnvelopeUseCase.invoke(FAKE_ENVELOPE)
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }
}
