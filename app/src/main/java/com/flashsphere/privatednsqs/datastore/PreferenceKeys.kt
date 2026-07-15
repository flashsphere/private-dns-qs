package com.flashsphere.privatednsqs.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    private const val SETTINGS_VERSION = 2

    val VERSION = PreferenceKey(intPreferencesKey("version"), SETTINGS_VERSION)
    val DNS_OFF_TOGGLE = PreferenceKey(booleanPreferencesKey("toggle_off"), true)
    val DNS_AUTO_TOGGLE = PreferenceKey(booleanPreferencesKey("toggle_auto"), true)
    val REQUIRE_UNLOCK = PreferenceKey(booleanPreferencesKey("require_unlock"), false)
    val DNS_PROVIDERS = PreferenceKey(stringPreferencesKey("dns_providers"), "[]")
    val ID_SEQUENCE = PreferenceKey(longPreferencesKey("id_sequence"), 0)
    val IMAGE_ID_SEQUENCE = PreferenceKey(longPreferencesKey("image_id_sequence"), 0)
    val SHOW_IN_TILE_TITLE = PreferenceKey(booleanPreferencesKey("show_in_tile_title"), false)
    val DNS_AUTO_AS_INACTIVE_TILE = PreferenceKey(booleanPreferencesKey("dns_auto_as_inactive_tile"), false)

    @Deprecated("Not used since migrating to datastore")
    val FIRST_RUN = booleanPreferencesKey("first_run")
    @Deprecated("Deprecated since version 2")
    val DNS_ON_TOGGLE = booleanPreferencesKey("toggle_on")
}

class PreferenceKey<T>(
    val key: Preferences.Key<T>,
    val defaultValue: T,
)