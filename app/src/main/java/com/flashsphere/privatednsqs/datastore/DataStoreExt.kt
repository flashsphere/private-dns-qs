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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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

fun <T> DataStore<Preferences>.getBlocking(pref: PreferenceKey<T>): T {
    return runBlocking {
        data.map { it[pref.key] ?: pref.defaultValue }.first()
    }
}

fun <T> DataStore<Preferences>.updateBlocking(pref: PreferenceKey<T>, value: T) {
    runBlocking {
        edit { it[pref.key] = value }
    }
}

fun DataStore<Preferences>.dnsOffToggle(): Boolean {
    return getBlocking(PreferenceKeys.DNS_OFF_TOGGLE)
}
fun DataStore<Preferences>.dnsOffToggle(value: Boolean) {
    return updateBlocking(PreferenceKeys.DNS_OFF_TOGGLE, value)
}

fun DataStore<Preferences>.dnsAutoToggle(): Boolean {
    return getBlocking(PreferenceKeys.DNS_AUTO_TOGGLE)
}
fun DataStore<Preferences>.dnsAutoToggle(value: Boolean) {
    return updateBlocking(PreferenceKeys.DNS_AUTO_TOGGLE, value)
}

fun DataStore<Preferences>.dnsOnToggle(): Boolean {
    return getBlocking(PreferenceKeys.DNS_ON_TOGGLE)
}
fun DataStore<Preferences>.dnsOnToggle(value: Boolean) {
    return updateBlocking(PreferenceKeys.DNS_ON_TOGGLE, value)
}

fun DataStore<Preferences>.requireUnlock(): Boolean {
    return getBlocking(PreferenceKeys.REQUIRE_UNLOCK)
}
fun DataStore<Preferences>.requireUnlock(value: Boolean) {
    return updateBlocking(PreferenceKeys.REQUIRE_UNLOCK, value)
}
