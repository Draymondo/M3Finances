package com.naveenapps.expensemanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.Date

/**
 * Persistance de la clé API et du cache de taux du "Convertisseur de devises" dans le
 * DataStore partagé de l'application.
 *
 * Un seul jeu de taux est mis en cache à la fois — celui de la dernière devise source
 * consultée. Si l'utilisateur change de devise source puis revient à une devise déjà utilisée
 * avant, mais entre-temps sans réseau, le cache de cette ancienne devise n'est plus disponible
 * (c'est un choix volontaire pour rester simple ; voir AGENTS_currency_converter.md).
 */
class CurrencyConverterDataStore(private val dataStore: DataStore<Preferences>) {

    suspend fun setApiKey(apiKey: String) {
        dataStore.edit { prefs -> prefs[KEY_API_KEY] = apiKey }
    }

    fun getApiKey(): Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_API_KEY] }

    suspend fun cacheRates(rates: CurrencyConversionRates) {
        dataStore.edit { prefs ->
            prefs[KEY_CACHE_BASE_CODE] = rates.baseCode
            prefs[KEY_CACHE_RATES_JSON] = Json.encodeToString(rates.rates)
            prefs[KEY_CACHE_LAST_UPDATED] = rates.lastUpdated.time
        }
    }

    fun getCachedRates(baseCode: String): Flow<CurrencyConversionRates?> =
        dataStore.data.map { prefs ->
            val cachedBaseCode = prefs[KEY_CACHE_BASE_CODE]
            val ratesJson = prefs[KEY_CACHE_RATES_JSON]
            val lastUpdated = prefs[KEY_CACHE_LAST_UPDATED]
            if (cachedBaseCode != baseCode || ratesJson == null || lastUpdated == null) {
                null
            } else {
                try {
                    CurrencyConversionRates(
                        baseCode = cachedBaseCode,
                        rates = Json.decodeFromString<Map<String, Double>>(ratesJson),
                        lastUpdated = Date(lastUpdated),
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("currency_converter_api_key")
        private val KEY_CACHE_BASE_CODE = stringPreferencesKey("currency_converter_cache_base_code")
        private val KEY_CACHE_RATES_JSON = stringPreferencesKey("currency_converter_cache_rates_json")
        private val KEY_CACHE_LAST_UPDATED = longPreferencesKey("currency_converter_cache_last_updated")
    }
}
