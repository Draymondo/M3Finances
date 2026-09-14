package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SetCurrencyConverterApiKeyUseCaseTest : BaseCoroutineTest() {

    private val repository: CurrencyConverterRepository = mock()
    private val setCurrencyConverterApiKeyUseCase = SetCurrencyConverterApiKeyUseCase(repository)

    @Test
    fun whenApiKeyIsBlankShouldReturnError() = runTest {
        val response = setCurrencyConverterApiKeyUseCase.invoke("   ")
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenApiKeyIsValidShouldTrimAndSaveIt() = runTest {
        val response = setCurrencyConverterApiKeyUseCase.invoke("  my-key-123  ")
        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
        verify(repository).setApiKey("my-key-123")
    }
}
