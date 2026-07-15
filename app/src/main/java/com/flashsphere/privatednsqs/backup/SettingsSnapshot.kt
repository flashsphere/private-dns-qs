package com.flashsphere.privatednsqs.backup

import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("version")
sealed interface SettingsSnapshot

@Serializable
@SerialName("1")
data class SettingsSnapshotV1(
    val dnsOffToggle: Boolean,
    val dnsAutoToggle: Boolean,
    val requireUnlock: Boolean,
    val showInTileTitle: Boolean = PreferenceKeys.SHOW_IN_TILE_TITLE.defaultValue,
    val dnsAutoAsInactiveTile: Boolean = PreferenceKeys.DNS_AUTO_AS_INACTIVE_TILE.defaultValue,
    val dnsProviders: List<DnsProviderSnapshot>
) : SettingsSnapshot
