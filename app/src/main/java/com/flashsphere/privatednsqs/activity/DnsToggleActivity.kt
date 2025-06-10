package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.core.content.pm.ShortcutManagerCompat
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
        reportShortcutUsed()
        if (privateDns.hasPermission()) {
            executeDnsMode()
        } else {
            showMessage(NoPermissionMessage)
        }
        finish()
    }

    private fun reportShortcutUsed() {
        val shortcutId = intent?.action
        when (shortcutId) {
            "privatedns.shortcut.off",
            "privatedns.shortcut.auto",
            "privatedns.shortcut.on" -> {
                ShortcutManagerCompat.reportShortcutUsed(this, shortcutId)
            }
            else -> {}
        }
    }
}