package com.flashsphere.privatednsqs.datastore

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_MODE
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER

class PrivateDns(
    private val context: Context
) {
    private val contentResolver = context.contentResolver

    fun hasPermission(): Boolean {
        return context.checkCallingOrSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_DENIED
    }

    fun getDnsMode(): DnsMode {
        val setting = Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)

        if (DnsMode.On.value.equals(setting, ignoreCase = true)) {
            return DnsMode.On
        }
        if (DnsMode.Auto.value.equals(setting, ignoreCase = true)) {
            return DnsMode.Auto
        }
        if (DnsMode.Off.value.equals(setting, ignoreCase = true)) {
            return DnsMode.Off
        }
        return DnsMode.Auto
    }

    fun setDnsMode(dnsMode: DnsMode) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, dnsMode.value)
    }

    fun getHostname(): String? {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
    }

    fun setHostname(hostname: String) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, hostname)
    }
}

enum class DnsMode(val value: String) {
    Off("off"), Auto("opportunistic"), On("hostname")
}