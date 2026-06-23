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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import timber.log.Timber

@OptIn(ExperimentalSerializationApi::class)
private val jsonInstance: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        exceptionsWithDebugInfo = true
    }
}
val Context.json: Json
    get() = jsonInstance

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
    return data
        .map { it[pref.key] ?: pref.defaultValue }
        .onEmpty { emit(pref.defaultValue) }
        .distinctUntilChanged()
}
fun <T> DataStore<Preferences>.getStateFlow(scope: CoroutineScope, pref: PreferenceKey<T>): StateFlow<T> {
    return getFlow(pref).stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = pref.defaultValue,
    )
}
fun DataStore<Preferences>.dnsOffToggleFlow(): Flow<Boolean> {
    return getFlow(PreferenceKeys.DNS_OFF_TOGGLE)
}
suspend fun DataStore<Preferences>.dnsOffToggle(value: Boolean) {
    return update(PreferenceKeys.DNS_OFF_TOGGLE, value)
}
fun DataStore<Preferences>.dnsAutoToggleFlow(): Flow<Boolean> {
    return getFlow(PreferenceKeys.DNS_AUTO_TOGGLE)
}
suspend fun DataStore<Preferences>.dnsAutoToggle(value: Boolean) {
    return update(PreferenceKeys.DNS_AUTO_TOGGLE, value)
}
suspend fun DataStore<Preferences>.requireUnlock(): Boolean {
    return get(PreferenceKeys.REQUIRE_UNLOCK)
}
suspend fun DataStore<Preferences>.requireUnlock(value: Boolean) {
    return update(PreferenceKeys.REQUIRE_UNLOCK, value)
}
fun DataStore<Preferences>.dnsProvidersFlow(): Flow<List<DnsProvider>> {
    return getFlow(PreferenceKeys.DNS_PROVIDERS)
        .map {
            withContext(Dispatchers.Default) {
                runCatching { jsonInstance.decodeFromString<List<DnsProvider>>(it) }
                    .getOrElse { e ->
                        Timber.e(e, "Failed to decode json")
                        emptyList()
                    }
            }
        }
}
fun DataStore<Preferences>.enabledDnsProvidersFlow(): Flow<Sequence<DnsProvider>> {
    return dnsProvidersFlow().map { list ->
        list.asSequence()
            .filter { it.enabled }
    }
}
suspend fun DataStore<Preferences>.dnsProviders(dnsProviders: List<DnsProvider>) {
    val json = withContext(Dispatchers.Default) {
        jsonInstance.encodeToString(dnsProviders)
    }
    return update(PreferenceKeys.DNS_PROVIDERS, json)
}
fun DataStore<Preferences>.dnsConfigurationsFlow(): Flow<List<DnsConfiguration>> {
    return combine(
        dnsOffToggleFlow(),
        dnsAutoToggleFlow(),
        enabledDnsProvidersFlow(),
    ) { dnsOffToggle, dnsAutoToggle, dnsProviders ->
        mutableListOf<DnsConfiguration>().also { configs ->
            if (dnsOffToggle) {
                configs.add(DnsConfiguration.Off)
            }
            if (dnsAutoToggle) {
                configs.add(DnsConfiguration.Auto)
            }
            configs.addAll(dnsProviders.map { DnsConfiguration.On(it.hostname, it.icon) })
        }
    }.onEmpty { emptyList<DnsConfiguration>() }
}
