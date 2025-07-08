package com.flashsphere.privatednsqs.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn

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
    return getFlow(pref).first()
}
fun <T> DataStore<Preferences>.getFlow(pref: PreferenceKey<T>): Flow<T> {
    return data.map { it[pref.key] ?: pref.defaultValue }.onEmpty { emit(pref.defaultValue) }
}
fun <T> DataStore<Preferences>.getStateFlow(scope: CoroutineScope, pref: PreferenceKey<T>): StateFlow<T> {
    return getFlow(pref).stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = pref.defaultValue,
    )
}
suspend fun DataStore<Preferences>.dnsOffToggle(): Boolean {
    return get(PreferenceKeys.DNS_OFF_TOGGLE)
}
suspend fun DataStore<Preferences>.dnsOffToggle(value: Boolean) {
    return update(PreferenceKeys.DNS_OFF_TOGGLE, value)
}
suspend fun DataStore<Preferences>.dnsAutoToggle(): Boolean {
    return get(PreferenceKeys.DNS_AUTO_TOGGLE)
}
suspend fun DataStore<Preferences>.dnsAutoToggle(value: Boolean) {
    return update(PreferenceKeys.DNS_AUTO_TOGGLE, value)
}
suspend fun DataStore<Preferences>.dnsOnToggle(): Boolean {
    return get(PreferenceKeys.DNS_ON_TOGGLE)
}
suspend fun DataStore<Preferences>.dnsOnToggle(value: Boolean) {
    return update(PreferenceKeys.DNS_ON_TOGGLE, value)
}
suspend fun DataStore<Preferences>.requireUnlock(): Boolean {
    return get(PreferenceKeys.REQUIRE_UNLOCK)
}
suspend fun DataStore<Preferences>.requireUnlock(value: Boolean) {
    return update(PreferenceKeys.REQUIRE_UNLOCK, value)
}
