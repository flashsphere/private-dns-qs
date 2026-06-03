package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.enabledDnsProvidersFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DnsOnActivity : DnsShortcutActivity() {
    override val showToastAfterSet: Boolean = true

    override fun getDnsConfig(): DnsConfiguration? {
        val configs = runBlocking {
            dataStore.enabledDnsProvidersFlow().first()
                .map { DnsConfiguration.On(it.hostname) }
                .toList()
        }
        return privateDns.getNextDnsConfig(configs)
    }
}