package com.flashsphere.privatednsqs.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.datastore.IdGenerator
import com.flashsphere.privatednsqs.datastore.ImageIdGenerator
import com.flashsphere.privatednsqs.datastore.PreferenceKey
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.datastore.get
import com.flashsphere.privatednsqs.datastore.update
import com.flashsphere.privatednsqs.hilt.ComputeDispatcher
import com.flashsphere.privatednsqs.util.DnsConfiguration
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    @ComputeDispatcher private val computeDispatcher: CoroutineDispatcher,
) {
    suspend fun getNextId(): Long {
        return IdGenerator.getNextId(dataStore)
    }

    suspend fun getNextImageId(): Long {
        return ImageIdGenerator.getNextId(dataStore)
    }

    private fun <T> getFlow(pref: PreferenceKey<T>): Flow<T> {
        return dataStore.data
            .map { it[pref.key] ?: pref.defaultValue }
            .distinctUntilChanged()
    }

    fun <T> getStateFlow(
        coroutineScope: CoroutineScope,
        pref: PreferenceKey<T>,
        started: SharingStarted = SharingStarted.Eagerly,
    ): StateFlow<T> {
        return getFlow(pref)
            .stateIn(
                scope = coroutineScope,
                started = started,
                initialValue = pref.defaultValue,
            )
    }

    private suspend fun <T> update(pref: PreferenceKey<T>, value: T) {
        dataStore.edit { it[pref.key] = value }
    }

    fun getDnsOffToggleFlow(): Flow<Boolean> {
        return getFlow(PreferenceKeys.DNS_OFF_TOGGLE)
    }

    suspend fun getDnsOffToggle(): Boolean {
        return dataStore.get(PreferenceKeys.DNS_OFF_TOGGLE)
    }

    suspend fun updateDnsOffToggle(value: Boolean) {
        return update(PreferenceKeys.DNS_OFF_TOGGLE, value)
    }

    fun getDnsAutoToggleFlow(): Flow<Boolean> {
        return getFlow(PreferenceKeys.DNS_AUTO_TOGGLE)
    }

    suspend fun getDnsAutoToggle(): Boolean {
        return dataStore.get(PreferenceKeys.DNS_AUTO_TOGGLE)
    }

    suspend fun updateDnsAutoToggle(value: Boolean) {
        return update(PreferenceKeys.DNS_AUTO_TOGGLE, value)
    }

    suspend fun getRequireUnlock(): Boolean {
        return dataStore.get(PreferenceKeys.REQUIRE_UNLOCK)
    }

    suspend fun updateRequireUnlock(value: Boolean) {
        return update(PreferenceKeys.REQUIRE_UNLOCK, value)
    }

    suspend fun getShowInTileTitle(): Boolean {
        return dataStore.get(PreferenceKeys.SHOW_IN_TILE_TITLE)
    }

    suspend fun updateShowInTileTitle(value: Boolean) {
        return update(PreferenceKeys.SHOW_IN_TILE_TITLE, value)
    }

    fun getDnsProvidersFlow(): Flow<List<DnsProvider>> {
        return getFlow(PreferenceKeys.DNS_PROVIDERS)
            .map {
                withContext(computeDispatcher) {
                    runCatching { json.decodeFromString<List<DnsProvider>>(it) }
                        .getOrElse { e ->
                            Timber.e(e, "Failed to decode json")
                            emptyList()
                        }
                }
            }
    }

    suspend fun getDnsProviders(): List<DnsProvider> {
        return getDnsProvidersFlow().first()
    }

    fun getEnabledDnsProvidersFlow(): Flow<Sequence<DnsProvider>> {
        return getDnsProvidersFlow().map { list ->
            list.asSequence().filter { it.enabled }
        }
    }

    suspend fun updateDnsProviders(dnsProviders: List<DnsProvider>) {
        val json = withContext(computeDispatcher) {
            json.encodeToString(dnsProviders)
        }
        dataStore.update(PreferenceKeys.DNS_PROVIDERS, json)
    }

    fun getDnsConfigurationsFlow(): Flow<List<DnsConfiguration>> {
        return combine(
            getDnsOffToggleFlow(),
            getDnsAutoToggleFlow(),
            getEnabledDnsProvidersFlow(),
        ) { dnsOffToggle, dnsAutoToggle, dnsProviders ->
            mutableListOf<DnsConfiguration>().apply {
                if (dnsOffToggle) {
                    add(DnsConfiguration.Off)
                }
                if (dnsAutoToggle) {
                    add(DnsConfiguration.Auto)
                }
                addAll(dnsProviders.map { DnsConfiguration.On(it.hostname, it.icon) })
            }
        }.onEmpty { emptyList<DnsConfiguration>() }
    }
}
