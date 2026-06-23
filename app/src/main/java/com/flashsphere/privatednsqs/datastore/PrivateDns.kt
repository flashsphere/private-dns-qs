package com.flashsphere.privatednsqs.datastore

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_MODE
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER

class PrivateDns(
    private val context: Context
) {
    private val contentResolver = context.contentResolver

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentDnsConfig(configs: List<DnsConfiguration>): DnsConfiguration {
        val setting = Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)

        if (DnsMode.On.value.equals(setting, ignoreCase = true)) {
            val hostname = getHostname() ?: ""

            return configs.filterIsInstance<DnsConfiguration.On>()
                .firstOrNull {
                    it.hostname.equals(hostname, ignoreCase = true)
                } ?: DnsConfiguration.On(hostname = hostname, icon = null)
        }
        if (DnsMode.Auto.value.equals(setting, ignoreCase = true)) {
            return DnsConfiguration.Auto
        }
        if (DnsMode.Off.value.equals(setting, ignoreCase = true)) {
            return DnsConfiguration.Off
        }
        return DnsConfiguration.Auto
    }

    fun getNextDnsConfig(configs: List<DnsConfiguration>): DnsConfiguration? {
        if (configs.isEmpty()) return null

        val currentConfig = getCurrentDnsConfig(configs)
        val index = configs.indexOf(currentConfig)
        val nextIndex = if (index == -1) 0 else (index + 1) % configs.size
        return configs[nextIndex]
    }

    fun setDnsConfig(dnsConfig: DnsConfiguration) {
        if (dnsConfig is DnsConfiguration.On) {
            Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER,
                dnsConfig.hostname)
        }
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, dnsConfig.mode.value)
    }

    fun getHostname(): String? {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
    }
}
