package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.util.DnsConfiguration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class DnsOnActivity : DnsShortcutActivity() {
    override val showToastAfterSet: Boolean = true

    override fun getDnsConfig(): DnsConfiguration? {
        val configs = runBlocking {
            settingsRepository.getDnsProvidersFlow().first()
                .map { DnsConfiguration.On(it.hostname, it.label, it.icon) }
                .toList()
        }
        val specificHostname = intent?.getStringExtra("hostname")
        if (specificHostname != null) {
            return configs.firstOrNull { it.hostname == specificHostname }
        }

        return privateDns.getNextDnsConfig(configs)
    }
}