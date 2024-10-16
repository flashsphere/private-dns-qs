package com.flashsphere.privatednsqs.activity

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.content.pm.ShortcutManagerCompat
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

        startActivity(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

        when (action) {
            ACTION_OFF -> {
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, SHORTCUT_OFF)
                DnsOffActivity.startActivity(this)
            }
            ACTION_AUTO -> {
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, SHORTCUT_AUTO)
                DnsAutoActivity.startActivity(this)
            }
            ACTION_ON -> {
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, SHORTCUT_ON)
                DnsOnActivity.startActivity(this)
            }
            else -> {}
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