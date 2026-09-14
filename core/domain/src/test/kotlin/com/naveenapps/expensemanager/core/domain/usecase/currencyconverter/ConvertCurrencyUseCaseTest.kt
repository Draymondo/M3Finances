package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource
import org.junit.Test
import java.util.Date

class ConvertCurrencyUseCaseTest {

    private val convertCurrencyUseCase = ConvertCurrencyUseCase()

    private val rates = CurrencyConversionRates(
        baseCode = "USD",
        rates = mapOf("USD" to 1.0, "EUR" to 0.92, "XOF" to 601.6),
        lastUpdated = Date(),
    )

    @Test
    fun whenAmountAndCurrencyAreValidShouldReturnConvertedAmount() {
        val response = convertCurrencyUseCase.invoke(amount = 10.0, toCode = "XOF", rates = rates)
        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
        Truth.assertThat((response as Resource.Success).data).isEqualTo(6016.0)
    }

    @Test
    fun whenAmountIsZeroOrNegativeShouldReturnError() {
        val response = convertCurrencyUseCase.invoke(amount = 0.0, toCode = "XOF", rates = rates)
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenTargetCurrencyIsMissingFromRatesShouldReturnError() {
        val response = convertCurrencyUseCase.invoke(amount = 10.0, toCode = "JPY", rates = rates)
        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenTargetCurrencyIsTheBaseCodeItselfShouldReturnSameAmount() {
        val ratesWithoutSelf = rates.copy(rates = mapOf("EUR" to 0.92))
        val response = convertCurrencyUseCase.invoke(
            amount = 10.0,
            toCode = "USD",
            rates = ratesWithoutSelf,
        )
        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
        Truth.assertThat((response as Resource.Success).data).isEqualTo(10.0)
    }
}
