package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage

abstract class DnsToggleActivity : BaseActivity() {
    abstract val dnsMode: DnsMode

    protected lateinit var privateDns: PrivateDns

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        privateDns = PrivateDns(this)
    }

    open fun executeDnsMode() {
        privateDns.setDnsMode(dnsMode)
    }

    fun showMessage(message: SnackbarMessage) {
        MainActivity.startActivity(this, message)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        if (privateDns.hasPermission()) {
            executeDnsMode()
        } else {
            showMessage(NoPermissionMessage)
        }
        finishAffinity()
    }
}