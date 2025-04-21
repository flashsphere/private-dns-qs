package com.flashsphere.privatednsqs.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

class SettingsMigration(private val context: Context) : DataMigration<Preferences> {
    override suspend fun cleanUp() {
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[PreferenceKeys.VERSION.key]
        return version == null || version < PreferenceKeys.VERSION.defaultValue
    }

    @Suppress("DEPRECATION")
    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutablePrefs = currentData.toMutablePreferences()

        if (mutablePrefs.contains(PreferenceKeys.FIRST_RUN)) {
            mutablePrefs.remove(PreferenceKeys.FIRST_RUN)
        }

        mutablePrefs[PreferenceKeys.VERSION.key] = PreferenceKeys.VERSION.defaultValue
        return mutablePrefs.toPreferences()
    }
}