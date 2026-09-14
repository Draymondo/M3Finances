package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface CurrencyConverterRepository {

    fun getApiKey(): Flow<String?>

    suspend fun setApiKey(apiKey: String)

    /** Dernier jeu de taux connu pour [baseCode], ou `null` si aucun cache pour cette devise. */
    fun getCachedRates(baseCode: String): Flow<CurrencyConversionRates?>

    /** Appelle exchangerate-api.com pour [baseCode] et met à jour le cache en cas de succès. */
    suspend fun refreshRates(baseCode: String): Resource<CurrencyConversionRates>
}
