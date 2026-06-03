package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsConfiguration

class DnsOffActivity : DnsShortcutActivity() {
    override fun getDnsConfig(): DnsConfiguration {
        return DnsConfiguration.Off
    }
}