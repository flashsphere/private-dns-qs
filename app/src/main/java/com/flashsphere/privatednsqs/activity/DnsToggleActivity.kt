package com.flashsphere.privatednsqs.activity

import androidx.annotation.CallSuper
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage
import kotlinx.coroutines.runBlocking

class DnsToggleActivity : DnsShortcutActivity() {

    @CallSuper
    override fun onStart() {
        super.onStart()
        reportShortcutUsed()
        executeDnsMode()
        finish()
    }

    private fun executeDnsMode() {
        val privateDns = PrivateDns(this)
        val nextDnsMode = runBlocking { privateDns.getNextDnsMode() }

        when (nextDnsMode) {
            DnsMode.Off, DnsMode.Auto -> {
                privateDns.setDnsMode(nextDnsMode)
                showToast(getString(nextDnsMode.labelResId))
            }
            DnsMode.On -> {
                val hostname = privateDns.getHostname()
                if (!hostname.isNullOrEmpty()) {
                    privateDns.setDnsMode(nextDnsMode)
                    showToast(hostname)
                } else {
                    showMessage(NoDnsHostnameMessage)
                }
            }
        }
    }

    private fun showToast(message: String) {
        val application = this.application
        if (application is PrivateDnsApplication) {
            application.showToast(message)
        }
    }
}