package com.flashsphere.privatednsqs.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flashsphere.privatednsqs.shortcut.ShortcutManager
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocaleChangedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var shortcutManager: ShortcutManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCALE_CHANGED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                shortcutManager.updateShortcuts()
            } finally {
                pendingResult.finish()
            }
        }
    }
}