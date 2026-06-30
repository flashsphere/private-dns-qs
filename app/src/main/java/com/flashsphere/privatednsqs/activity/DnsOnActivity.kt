package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DnsOnActivity : DnsShortcutActivity() {
    override val showToastAfterSet: Boolean = true

    override fun getDnsConfig(): DnsConfiguration? {
        val configs = runBlocking {
            settingsRepository.getEnabledDnsProvidersFlow().first()
                .map { DnsConfiguration.On(it.hostname, it.icon) }
                .toList()
        }
        return privateDns.getNextDnsConfig(configs)
    }
}