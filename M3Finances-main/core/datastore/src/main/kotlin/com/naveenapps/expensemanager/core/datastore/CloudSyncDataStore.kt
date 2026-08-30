package com.naveenapps.expensemanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CloudSyncDataStore(private val dataStore: DataStore<Preferences>) {

    fun observeUnresolvedConflict(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_UNRESOLVED_CONFLICT] ?: false
    }

    suspend fun getLastLocalSyncedAt(): Long =
        dataStore.data.first()[KEY_LAST_LOCAL_SYNCED_AT] ?: 0L

    suspend fun hasLocalChangesSinceSync(): Boolean =
        dataStore.data.first()[KEY_LOCAL_CHANGES_SINCE_SYNC] ?: false

    suspend fun hasUnresolvedConflict(): Boolean =
        dataStore.data.first()[KEY_UNRESOLVED_CONFLICT] ?: false

    suspend fun setLastLocalSyncedAt(millis: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_LOCAL_SYNCED_AT] = millis
            prefs[KEY_LOCAL_CHANGES_SINCE_SYNC] = false
            prefs[KEY_UNRESOLVED_CONFLICT] = false
        }
    }

    suspend fun markLocalChanged() {
        dataStore.edit { prefs ->
            prefs[KEY_LOCAL_CHANGES_SINCE_SYNC] = true
        }
    }

    suspend fun setUnresolvedConflict(conflict: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_UNRESOLVED_CONFLICT] = conflict
        }
    }

    companion object {
        private val KEY_LAST_LOCAL_SYNCED_AT = longPreferencesKey("cloud_last_local_synced_at")
        private val KEY_LOCAL_CHANGES_SINCE_SYNC =
            booleanPreferencesKey("cloud_local_changes_since_sync")
        private val KEY_UNRESOLVED_CONFLICT = booleanPreferencesKey("cloud_unresolved_conflict")
    }
}
