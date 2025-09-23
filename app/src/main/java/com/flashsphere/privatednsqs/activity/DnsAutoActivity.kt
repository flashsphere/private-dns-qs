package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsMode

class DnsAutoActivity : DnsModeActivity() {
    override val dnsMode = DnsMode.Auto
}