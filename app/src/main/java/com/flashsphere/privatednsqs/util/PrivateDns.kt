package com.flashsphere.privatednsqs.util

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_MODE
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PrivateDns @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val contentResolver = context.contentResolver

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentDnsConfig(configs: List<DnsConfiguration>): DnsConfiguration {
        return when (getMode(contentResolver)) {
            DnsMode.On.value -> {
                val hostname = getHostname(contentResolver).orEmpty()
                configs.asSequence()
                    .filterIsInstance<DnsConfiguration.On>()
                    .firstOrNull {
                        it.hostname.equals(hostname, ignoreCase = true)
                    } ?: DnsConfiguration.On(hostname = hostname, icon = null)
            }
            DnsMode.Auto.value -> DnsConfiguration.Auto
            DnsMode.Off.value -> DnsConfiguration.Off
            else -> DnsConfiguration.Auto
        }
    }

    fun getNextDnsConfig(configs: List<DnsConfiguration>): DnsConfiguration? {
        if (configs.isEmpty()) return null

        val index = when (getMode(contentResolver)) {
            DnsMode.On.value -> {
                val hostname = getHostname(contentResolver).orEmpty()
                configs.indexOfFirst {
                    it is DnsConfiguration.On && it.hostname.equals(hostname, ignoreCase = true)
                }
            }
            DnsMode.Auto.value -> configs.indexOf(DnsConfiguration.Auto)
            DnsMode.Off.value -> configs.indexOf(DnsConfiguration.Off)
            else -> -1
        }

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

    companion object {
        fun getMode(contentResolver: ContentResolver): String? {
            return Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)?.lowercase()
        }
        fun getHostname(contentResolver: ContentResolver): String? {
            return Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
        }
    }
}
