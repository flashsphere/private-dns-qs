package com.flashsphere.privatednsqs.activity

import androidx.core.content.pm.ShortcutManagerCompat
import com.flashsphere.privatednsqs.ui.SnackbarMessage

abstract class DnsShortcutActivity : BaseActivity() {

    protected fun showMessage(message: SnackbarMessage) {
        MainActivity.startActivity(this, message)
    }

    protected fun reportShortcutUsed() {
        val shortcutId = intent?.action
        when (shortcutId) {
            "privatedns.shortcut.toggle",
            "privatedns.shortcut.off",
            "privatedns.shortcut.auto",
            "privatedns.shortcut.on" -> {
                ShortcutManagerCompat.reportShortcutUsed(this, shortcutId)
            }
            else -> {}
        }
    }
}