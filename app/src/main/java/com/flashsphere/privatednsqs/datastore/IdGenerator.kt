package com.flashsphere.privatednsqs.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object IdGenerator {
    private val mutex = Mutex()

    suspend fun getNextId(dataStore: DataStore<Preferences>): Int = mutex.withLock {
        val id = dataStore.get(PreferenceKeys.ID_SEQUENCE) + 1
        dataStore.update(PreferenceKeys.ID_SEQUENCE, id)
        id
    }

    suspend fun getNextId(prefs: MutablePreferences): Int = mutex.withLock {
        val id = (prefs[PreferenceKeys.ID_SEQUENCE.key] ?: PreferenceKeys.ID_SEQUENCE.defaultValue) + 1
        prefs[PreferenceKeys.ID_SEQUENCE.key] = id
        id
    }
}