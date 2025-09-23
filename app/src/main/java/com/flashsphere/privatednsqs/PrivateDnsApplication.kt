package com.flashsphere.privatednsqs

import android.app.Application
import android.widget.Toast
import com.jakewharton.processphoenix.ProcessPhoenix
import timber.log.Timber

class PrivateDnsApplication : Application() {
    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private var toast: Toast? = null

    override fun onCreate() {
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            return
        }
        super.onCreate()
    }

    fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .also { it.show() }
    }
}