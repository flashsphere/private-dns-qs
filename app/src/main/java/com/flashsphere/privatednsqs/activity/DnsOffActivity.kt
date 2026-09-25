package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.util.DnsConfiguration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DnsOffActivity : DnsShortcutActivity() {
    override val showToastAfterSet: Boolean = true

    override fun getDnsConfig(): DnsConfiguration {
        return DnsConfiguration.Off
    }
}