package com.flashsphere.privatednsqs.activity

import android.content.Context
import android.content.Intent
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage

class DnsOnActivity : DnsToggleActivity() {
    override val dnsMode = DnsMode.On

    override fun executeDnsMode(privateDns: PrivateDns) {
        val hostname = privateDns.getHostname()
        if (!hostname.isNullOrEmpty()) {
            privateDns.setDnsMode(dnsMode)
        } else {
            showMessage(NoDnsHostnameMessage)
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DnsOnActivity::class.java))
        }
    }
}