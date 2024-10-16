package com.flashsphere.privatednsqs.activity

import android.content.Context
import android.content.Intent
import com.flashsphere.privatednsqs.datastore.DnsMode

class DnsAutoActivity : DnsToggleActivity() {
    override val dnsMode = DnsMode.Auto

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DnsAutoActivity::class.java))
        }
    }
}