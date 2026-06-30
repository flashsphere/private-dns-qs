package com.flashsphere.privatednsqs.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
    ),
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "togglestates"),
            SettingsMigration(context),
        )
    }
)

suspend fun <T> DataStore<Preferences>.update(pref: PreferenceKey<T>, value: T) {
    edit { it[pref.key] = value }
}

suspend fun <T> DataStore<Preferences>.get(pref: PreferenceKey<T>): T {
    return data.first()[pref.key] ?: pref.defaultValue
}
