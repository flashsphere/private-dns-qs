package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.util.DnsConfiguration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class DnsToggleActivity : DnsShortcutActivity() {
    override val showToastAfterSet: Boolean = true

    override fun getDnsConfig(): DnsConfiguration? {
        val configs = runBlocking {
            settingsRepository.getDnsConfigurationsFlow().first()
        }
        return privateDns.getNextDnsConfig(configs)
    }
}