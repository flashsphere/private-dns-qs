package com.flashsphere.privatednsqs.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flashsphere.privatednsqs.util.ShortcutHelper
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocaleChangedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var shortcutHelper: ShortcutHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCALE_CHANGED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                shortcutHelper.populateShortcuts()
            } finally {
                pendingResult.finish()
            }
        }
    }
}