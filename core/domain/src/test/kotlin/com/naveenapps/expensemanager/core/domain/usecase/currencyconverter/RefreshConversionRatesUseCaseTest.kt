package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date

class RefreshConversionRatesUseCaseTest : BaseCoroutineTest() {

    private val repository: CurrencyConverterRepository = mock()
    private val refreshConversionRatesUseCase = RefreshConversionRatesUseCase(repository)

    @Test
    fun whenBaseCodeIsBlankShouldReturnErrorWithoutCallingRepository() = runTest {
        val response = refreshConversionRatesUseCase.invoke("")
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenBaseCodeIsValidShouldDelegateToRepository() = runTest {
        val expected = CurrencyConversionRates(
            baseCode = "USD",
            rates = mapOf("EUR" to 0.92),
            lastUpdated = Date(),
        )
        whenever(repository.refreshRates("USD")).thenReturn(Resource.Success(expected))

        val response = refreshConversionRatesUseCase.invoke("USD")

        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
        Truth.assertThat((response as Resource.Success).data).isEqualTo(expected)
    }
}
