package com.naveenapps.expensemanager.core.data.network.exchangerate

import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApiService {

    @GET("v6/{apiKey}/latest/{baseCode}")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCode") baseCode: String,
    ): ExchangeRatesResponse
}
