package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.util.DnsConfiguration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DnsAutoActivity : DnsShortcutActivity() {
    override fun getDnsConfig(): DnsConfiguration {
        return DnsConfiguration.Auto
    }
}