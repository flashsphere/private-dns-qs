package com.flashsphere.privatednsqs.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.privatednsqs.json.json

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

        if (!mutablePrefs.contains(PreferenceKeys.DNS_PROVIDERS.key)) {
            PrivateDns(context).getHostname()?.let { hostname ->
                if (hostname.isNotBlank()) {
                    val dnsProvider = DnsProvider(
                        id = IdGenerator.getNextId(mutablePrefs),
                        hostname = hostname,
                        icon = null,
                    )
                    mutablePrefs[PreferenceKeys.DNS_PROVIDERS.key] =
                        context.json.encodeToString(listOf(dnsProvider))
                }
            }
        }
        if (mutablePrefs.contains(PreferenceKeys.DNS_ON_TOGGLE)) {
            mutablePrefs.remove(PreferenceKeys.DNS_ON_TOGGLE)
        }

        mutablePrefs[PreferenceKeys.VERSION.key] = PreferenceKeys.VERSION.defaultValue
        return mutablePrefs.toPreferences()
    }
}