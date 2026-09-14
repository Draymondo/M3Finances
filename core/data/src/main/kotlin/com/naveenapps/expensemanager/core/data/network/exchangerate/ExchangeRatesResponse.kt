package com.naveenapps.expensemanager.core.data.network.exchangerate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Réponse de `GET https://v6.exchangerate-api.com/v6/{apiKey}/latest/{baseCode}`.
 * Doc : https://www.exchangerate-api.com/docs/standard-requests
 *
 * En cas d'erreur, `result == "error"` et `errorType` porte le code (ex: "invalid-key",
 * "inactive-account", "quota-reached", "unsupported-code") — `conversionRates` est alors null.
 */
@Serializable
data class ExchangeRatesResponse(
    val result: String,
    @SerialName("base_code") val baseCode: String? = null,
    @SerialName("conversion_rates") val conversionRates: Map<String, Double>? = null,
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @SerialName("error-type") val errorType: String? = null,
)
