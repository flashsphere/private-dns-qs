package com.flashsphere.privatednsqs.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ImageIdGenerator {
    private val mutex = Mutex()

    suspend fun getNextId(dataStore: DataStore<Preferences>): Long = mutex.withLock {
        val id = dataStore.get(PreferenceKeys.IMAGE_ID_SEQUENCE) + 1
        dataStore.update(PreferenceKeys.IMAGE_ID_SEQUENCE, id)
        id
    }
}