package com.flashsphere.privatednsqs.datastore

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_MODE
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import com.flashsphere.privatednsqs.R

class PrivateDns(
    private val context: Context
) {
    private val contentResolver = context.contentResolver

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
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

    suspend fun getNextDnsMode(): DnsMode {
        val dataStore = context.dataStore
        return when (getDnsMode()) {
            DnsMode.Off -> {
                if (dataStore.dnsAutoToggle()) {
                    DnsMode.Auto
                } else if (dataStore.dnsOnToggle()) {
                    DnsMode.On
                } else {
                    DnsMode.Off
                }
            }
            DnsMode.Auto -> {
                if (dataStore.dnsOnToggle()) {
                    DnsMode.On
                } else if (dataStore.dnsOffToggle()) {
                    DnsMode.Off
                } else {
                    DnsMode.Auto
                }
            }
            DnsMode.On -> {
                if (dataStore.dnsOffToggle()) {
                    DnsMode.Off
                } else if (dataStore.dnsAutoToggle()) {
                    DnsMode.Auto
                } else {
                    DnsMode.On
                }
            }
        }
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

enum class DnsMode(val value: String, val iconResId: Int, val labelResId: Int) {
    Off("off", R.drawable.ic_dns_off, R.string.off),
    Auto("opportunistic", R.drawable.ic_dns_auto, R.string.auto),
    On("hostname", R.drawable.ic_dns_on, R.string.on);
}