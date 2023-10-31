package com.flashsphere.privatednsqs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flashsphere.privatednsqs.service.PrivateDnsService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) {
            return
        }
        PrivateDnsService.start(context)
    }
}