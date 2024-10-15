package com.flashsphere.privatednsqs.activity

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.content.pm.ShortcutManagerCompat
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import timber.log.Timber

class ShortcutsActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        val action = intent?.action
        if (action.isNullOrEmpty()) {
            finish()
            return
        }
        Timber.d("intent action = %s", intent.action)

        val privateDns = PrivateDns(this)
        if (!privateDns.hasPermission()) {
            startActivity(MainActivity.getIntent(this, NoPermissionMessage))
            finish()
            return
        }
        val returnToHome = when (action) {
            ACTION_OFF -> {
                privateDns.setDnsMode(DnsMode.Off)
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, SHORTCUT_OFF)
                true
            }
            ACTION_AUTO -> {
                privateDns.setDnsMode(DnsMode.Auto)
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, SHORTCUT_AUTO)
                true
            }
            ACTION_ON -> {
                val hostname = privateDns.getHostname()
                if (!hostname.isNullOrEmpty()) {
                    privateDns.setDnsMode(DnsMode.On)
                    ShortcutManagerCompat.reportShortcutUsed(applicationContext, SHORTCUT_ON)
                    true
                } else {
                    startActivity(MainActivity.getIntent(this, NoDnsHostnameMessage))
                    false
                }
            }
            else -> { true }
        }

        if (returnToHome) {
            startActivity(Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    companion object {
        private const val ACTION_OFF = "actions.intent.OFF"
        private const val ACTION_AUTO = "actions.intent.AUTO"
        private const val ACTION_ON = "actions.intent.ON"

        private const val SHORTCUT_OFF = "privatedns.shortcut.off"
        private const val SHORTCUT_AUTO = "privatedns.shortcut.auto"
        private const val SHORTCUT_ON = "privatedns.shortcut.on"
    }
}