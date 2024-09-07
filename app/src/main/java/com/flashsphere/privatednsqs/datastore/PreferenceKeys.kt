package com.flashsphere.privatednsqs.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object PreferenceKeys {
    private const val SETTINGS_VERSION = 1

    val VERSION = PreferenceKey(intPreferencesKey("version"), SETTINGS_VERSION)
    val DNS_OFF_TOGGLE = PreferenceKey(booleanPreferencesKey("toggle_off"), true)
    val DNS_AUTO_TOGGLE = PreferenceKey(booleanPreferencesKey("toggle_auto"), true)
    val DNS_ON_TOGGLE = PreferenceKey(booleanPreferencesKey("toggle_on"), true)
    val REQUIRE_UNLOCK = PreferenceKey(booleanPreferencesKey("require_unlock"), false)

    @Deprecated("Not used since migrating to datastore")
    val FIRST_RUN = booleanPreferencesKey("first_run")
}

class PreferenceKey<T>(
    val key: Preferences.Key<T>,
    val defaultValue: T,
)