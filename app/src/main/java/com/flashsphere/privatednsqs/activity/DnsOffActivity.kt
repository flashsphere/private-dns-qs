package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsMode

class DnsOffActivity : DnsModeActivity() {
    override val dnsMode = DnsMode.Off
}