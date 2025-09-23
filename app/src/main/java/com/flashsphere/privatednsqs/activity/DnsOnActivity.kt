package com.flashsphere.privatednsqs.activity

import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage

class DnsOnActivity : DnsModeActivity() {
    override val dnsMode = DnsMode.On

    override fun executeDnsMode() {
        val hostname = privateDns.getHostname()
        if (!hostname.isNullOrEmpty()) {
            privateDns.setDnsMode(dnsMode)
        } else {
            showMessage(NoDnsHostnameMessage)
        }
    }
}