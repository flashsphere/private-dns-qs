package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.jakewharton.processphoenix.ProcessPhoenix

abstract class BaseActivity : ComponentActivity() {
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        // check if app is launched in restricted mode (due to auto backup) and restart the process
        // https://issuetracker.google.com/issues/160946170#comment8
        if (application !is PrivateDnsApplication) {
            ProcessPhoenix.triggerRebirth(this)
            return
        }
        super.onCreate(savedInstanceState)
    }
}