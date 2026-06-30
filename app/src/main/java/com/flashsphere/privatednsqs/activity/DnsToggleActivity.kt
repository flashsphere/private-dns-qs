package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DnsToggleActivity : DnsShortcutActivity() {
    override val showToastAfterSet: Boolean = true

    override fun getDnsConfig(): DnsConfiguration? {
        val configs = runBlocking {
            settingsRepository.getDnsConfigurationsFlow().first()
        }
        return privateDns.getNextDnsConfig(configs)
    }
}