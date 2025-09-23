package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.annotation.CallSuper
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.ui.NoPermissionMessage

abstract class DnsModeActivity : DnsShortcutActivity() {
    protected lateinit var privateDns: PrivateDns

    abstract val dnsMode: DnsMode

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        privateDns = PrivateDns(this)
    }

    open fun executeDnsMode() {
        privateDns.setDnsMode(dnsMode)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        reportShortcutUsed()
        if (privateDns.hasPermission()) {
            executeDnsMode()
        } else {
            showMessage(NoPermissionMessage)
        }
        finish()
    }
}