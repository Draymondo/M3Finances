package com.naveenapps.expensemanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ToolUsageDataStore(private val dataStore: DataStore<Preferences>) {

    fun getAllUsage(): Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        preferences.asMap().mapNotNull { (key, value) ->
            if (key.name.startsWith(KEY_PREFIX) && value is Int) {
                key.name.removePrefix(KEY_PREFIX) to value
            } else null
        }.toMap()
    }

    suspend fun incrementUsage(toolKey: String) {
        dataStore.edit { preferences ->
            val prefKey = intPreferencesKey("$KEY_PREFIX$toolKey")
            val current = preferences[prefKey] ?: 0
            preferences[prefKey] = current + 1
        }
    }

    suspend fun setAllUsage(usage: Map<String, Int>) {
        dataStore.edit { preferences ->
            usage.forEach { (toolKey, count) ->
                val prefKey = intPreferencesKey("$KEY_PREFIX$toolKey")
                val current = preferences[prefKey] ?: 0
                preferences[prefKey] = maxOf(current, count)
            }
        }
    }

    companion object {
        private const val KEY_PREFIX = "tool_usage_"
    }
}
