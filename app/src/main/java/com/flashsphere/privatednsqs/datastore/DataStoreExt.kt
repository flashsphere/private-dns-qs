package com.flashsphere.privatednsqs.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

suspend fun <T> DataStore<Preferences>.update(pref: PreferenceKey<T>, value: T) {
    edit { it[pref.key] = value }
}

suspend fun <T> DataStore<Preferences>.get(pref: PreferenceKey<T>): T {
    return data.first()[pref.key] ?: pref.defaultValue
}
