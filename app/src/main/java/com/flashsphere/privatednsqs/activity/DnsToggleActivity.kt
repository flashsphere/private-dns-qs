package com.flashsphere.privatednsqs.activity

import androidx.activity.ComponentActivity
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage

abstract class DnsToggleActivity : ComponentActivity() {
    abstract val dnsMode: DnsMode

    open fun executeDnsMode(privateDns: PrivateDns) {
        privateDns.setDnsMode(dnsMode)
    }

    fun showMessage(message: SnackbarMessage) {
        MainActivity.startActivity(this, message)
    }

    override fun onStart() {
        super.onStart()
        val privateDns = PrivateDns(this)
        if (privateDns.hasPermission()) {
            executeDnsMode(privateDns)
        } else {
            showMessage(NoPermissionMessage)
        }
        finishAffinity()
    }
}