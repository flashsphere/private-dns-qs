package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsConfiguration

class DnsAutoActivity : DnsShortcutActivity() {
    override fun getDnsConfig(): DnsConfiguration {
        return DnsConfiguration.Auto
    }
}