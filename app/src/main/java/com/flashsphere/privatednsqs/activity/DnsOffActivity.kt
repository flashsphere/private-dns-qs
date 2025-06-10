package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsMode

class DnsOffActivity : DnsToggleActivity() {
    override val dnsMode = DnsMode.Off
}