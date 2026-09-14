package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.data.network.exchangerate.ExchangeRateApiService
import com.naveenapps.expensemanager.core.datastore.CurrencyConverterDataStore
import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.CurrencyConverterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date

class CurrencyConverterRepositoryImpl(
    private val dataStore: CurrencyConverterDataStore,
    private val apiService: ExchangeRateApiService,
    private val dispatchers: AppCoroutineDispatchers,
) : CurrencyConverterRepository {

    override fun getApiKey(): Flow<String?> = dataStore.getApiKey()

    override suspend fun setApiKey(apiKey: String) {
        dataStore.setApiKey(apiKey)
    }

    override fun getCachedRates(baseCode: String): Flow<CurrencyConversionRates?> =
        dataStore.getCachedRates(baseCode)

    override suspend fun refreshRates(baseCode: String): Resource<CurrencyConversionRates> =
        withContext(dispatchers.io) {
            try {
                val apiKey = dataStore.getApiKey().first()
                if (apiKey.isNullOrBlank()) {
                    return@withContext Resource.Error(
                        Exception("Aucune clé API enregistrée — renseigne-la dans les réglages de l'outil"),
                    )
                }

                val response = apiService.getLatestRates(apiKey = apiKey, baseCode = baseCode)

                val rates = response.conversionRates
                if (response.result != "success" || rates == null) {
                    val message = when (response.errorType) {
                        "invalid-key" -> "Clé API invalide"
                        "inactive-account" ->
                            "Compte inactif — vérifie tes emails exchangerate-api.com pour l'activer"
                        "quota-reached" -> "Quota d'appels gratuits atteint pour ce mois"
                        "unsupported-code" -> "Devise non prise en charge par exchangerate-api.com"
                        else -> "Erreur lors de la récupération des taux"
                    }
                    return@withContext Resource.Error(Exception(message))
                }

                val lastUpdatedSeconds = response.timeLastUpdateUnix
                    ?: (System.currentTimeMillis() / 1000)
                val result = CurrencyConversionRates(
                    baseCode = response.baseCode ?: baseCode,
                    rates = rates,
                    lastUpdated = Date(lastUpdatedSeconds * 1000),
                )
                dataStore.cacheRates(result)
                Resource.Success(result)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }
        }
}
